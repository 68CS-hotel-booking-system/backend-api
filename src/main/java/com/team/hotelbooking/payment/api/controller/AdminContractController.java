package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.request.CreateContractRequest;
import com.team.hotelbooking.payment.api.dto.response.HotelContractDto;
import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.ActivateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.TerminateContractUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// PM-16: chỉ ADMIN tạo, kích hoạt và chấm dứt hợp đồng; không có API sửa điều khoản.
@RestController
@RequestMapping("/api/v1/admin/hotels")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
    name = "Finance - Contracts (Admin)",
    description = "API quản trị hợp đồng hoa hồng đối tác khách sạn")
public class AdminContractController {

  private final CreateContractUseCase createContractUseCase;
  private final ActivateContractUseCase activateContractUseCase;
  private final TerminateContractUseCase terminateContractUseCase;

  @Operation(
      summary = "[FN02] Tạo hợp đồng hoa hồng khách sạn (Admin)",
      description =
          "Chỉ ADMIN. Tạo hợp đồng hoa hồng mới với đối tác khách sạn. Kiểm tra chặt chẽ: mã hợp"
              + " đồng không trùng, tỷ lệ hoa hồng 0-100%, và không trùng lặp thời gian với hợp"
              + " đồng ACTIVE khác. Người tạo lấy từ tài khoản đăng nhập.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Tạo hợp đồng thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Dữ liệu hợp đồng hoặc khoảng thời gian không hợp lệ",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Không phải ADMIN",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy khách sạn hoặc chủ sở hữu (Owner)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Mã hợp đồng đã tồn tại hoặc trùng lặp thời gian với hợp đồng ACTIVE khác",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/contracts")
  public ResponseEntity<ApiResponse<HotelContractDto>> createContract(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Valid @RequestBody CreateContractRequest request,
      Authentication authentication) {

    HotelContractResult result =
        createContractUseCase.execute(
            new CreateContractCommand(
                hotelId,
                request.contractCode(),
                request.ownerUserId(),
                request.commissionRate(),
                request.commissionBasis(),
                request.startsAt(),
                request.endsAt(),
                request.status(),
                CurrentActor.userId(authentication)));

    HotelContractDto dto = HotelContractDto.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo hợp đồng hoa hồng thành công.", dto));
  }

  @Operation(
      summary = "[PM-16] Kích hoạt hợp đồng (DRAFT → ACTIVE)",
      description =
          "Chỉ ADMIN. Không cho hai hợp đồng ACTIVE của cùng khách sạn chồng khoảng hiệu lực"
              + " [startsAt, endsAt). Hợp đồng đã hết hiệu lực không kích hoạt được. Điều khoản"
              + " không đổi; muốn đổi thì tạo hợp đồng mới.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã kích hoạt"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Hợp đồng đã hết hiệu lực",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không có hợp đồng này trong khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Không ở DRAFT hoặc chồng hiệu lực với hợp đồng ACTIVE khác",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/contracts/{contractId}/activate")
  public ResponseEntity<ApiResponse<HotelContractDto>> activateContract(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("contractId") UUID contractId,
      Authentication authentication) {

    HotelContractResult result =
        activateContractUseCase.execute(
            new ChangeContractStatusCommand(
                CurrentActor.userId(authentication), hotelId, contractId));

    return ResponseEntity.ok(
        ApiResponse.success("Kích hoạt hợp đồng thành công.", HotelContractDto.from(result)));
  }

  @Operation(
      summary = "[PM-16] Chấm dứt hợp đồng (DRAFT/ACTIVE → TERMINATED)",
      description =
          "Chỉ ADMIN. Không sửa điều khoản hay khoảng hiệu lực; booking đã chốt giữ snapshot"
              + " hợp đồng cũ.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã chấm dứt"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không có hợp đồng này trong khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Hợp đồng đã EXPIRED hoặc TERMINATED",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/contracts/{contractId}/terminate")
  public ResponseEntity<ApiResponse<HotelContractDto>> terminateContract(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("contractId") UUID contractId,
      Authentication authentication) {

    HotelContractResult result =
        terminateContractUseCase.execute(
            new ChangeContractStatusCommand(
                CurrentActor.userId(authentication), hotelId, contractId));

    return ResponseEntity.ok(
        ApiResponse.success("Chấm dứt hợp đồng thành công.", HotelContractDto.from(result)));
  }
}

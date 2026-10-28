package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.request.CreateContractRequest;
import com.team.hotelbooking.payment.api.dto.response.HotelContractDto;
import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.domain.model.HotelContract;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hotels")
@RequiredArgsConstructor
@Tag(
    name = "Finance - Contracts (Admin)",
    description = "API quản trị hợp đồng hoa hồng đối tác khách sạn")
public class AdminContractController {

  private final CreateContractUseCase createContractUseCase;

  @Operation(
      summary = "[FN02] Tạo hợp đồng hoa hồng khách sạn (Admin)",
      description =
          "Tạo hợp đồng hoa hồng mới với đối tác khách sạn. Kiểm tra chặt chẽ: mã hợp đồng không trùng, "
              + "tỷ lệ hoa hồng 0-100%, và không trùng lặp thời gian với hợp đồng ACTIVE khác.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Tạo hợp đồng thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Dữ liệu hợp đồng hoặc khoảng thời gian không hợp lệ",
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
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<HotelContractDto>> createContract(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Valid @RequestBody CreateContractRequest request) {

    HotelContract contract =
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
                null));

    HotelContractDto dto =
        new HotelContractDto(
            contract.id(),
            contract.contractCode(),
            contract.hotelId(),
            contract.ownerUserId(),
            contract.commissionRate(),
            contract.commissionBasis(),
            contract.startsAt(),
            contract.endsAt(),
            contract.status(),
            contract.createdByUserId(),
            contract.createdAt(),
            contract.updatedAt());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo hợp đồng hoa hồng thành công.", dto));
  }
}

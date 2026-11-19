package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.ChangeHotelStatusRequest;
import com.team.hotelbooking.hotel.api.dto.response.ManagementHotelResponse;
import com.team.hotelbooking.hotel.application.command.ResumeHotelCommand;
import com.team.hotelbooking.hotel.application.command.SuspendHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ResumeHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.SuspendHotelUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HT-09: quản lý tạm ngừng và mở bán lại khách sạn được giao; quyền theo hotel kiểm trong use case
// (cùng quy tắc PATCH khách sạn). Không duyệt lần đầu, không gỡ LOCKED.
@RestController
@RequestMapping("/api/v1/management/hotels")
@RequiredArgsConstructor
public class ManagementHotelStatusController {

  private final SuspendHotelUseCase suspendHotelUseCase;
  private final ResumeHotelUseCase resumeHotelUseCase;

  @Operation(
      summary = "Tạm ngừng bán khách sạn (ACTIVE → SUSPENDED)",
      description =
          "ADMIN, Owner hoặc HOTEL_MANAGER đang hoạt động của đúng khách sạn. Chỉ chặn đơn mới;"
              + " đơn đã có vẫn được phục vụ (HT-06). Khách sạn LOCKED trả 409 HOTEL_LOCKED.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã tạm ngừng, trả trạng thái SUSPENDED"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Là thành viên khách sạn nhưng không đủ quyền (RECEPTIONIST)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại hoặc người gọi không thuộc khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Khách sạn không ở trạng thái ACTIVE hoặc đang bị khóa",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/suspend")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> suspendHotel(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody(required = false) ChangeHotelStatusRequest request,
      Authentication authentication) {
    SuspendHotelCommand command =
        new SuspendHotelCommand(
            CurrentActor.userId(authentication),
            CurrentActor.isAdmin(authentication),
            hotelId,
            request == null ? null : request.reason());

    ManagementHotelResult result = suspendHotelUseCase.execute(command);

    return ok("Tạm ngừng bán khách sạn thành công.", result);
  }

  @Operation(
      summary = "Mở bán lại khách sạn (SUSPENDED → ACTIVE)",
      description =
          "ADMIN, Owner hoặc HOTEL_MANAGER đang hoạt động của đúng khách sạn. Kiểm lại HT-03;"
              + " thiếu điều kiện trả 422. Khách sạn DRAFT (chưa công khai lần đầu) trả 409; khách"
              + " sạn LOCKED trả 409 HOTEL_LOCKED.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã mở bán lại, trả trạng thái ACTIVE"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Là thành viên khách sạn nhưng không đủ quyền (RECEPTIONIST)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại hoặc người gọi không thuộc khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Khách sạn không ở trạng thái SUSPENDED hoặc đang bị khóa",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "422",
        description = "Chưa đủ điều kiện mở bán theo HT-03",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/resume")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> resumeHotel(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody(required = false) ChangeHotelStatusRequest request,
      Authentication authentication) {
    ResumeHotelCommand command =
        new ResumeHotelCommand(
            CurrentActor.userId(authentication),
            CurrentActor.isAdmin(authentication),
            hotelId,
            request == null ? null : request.reason());

    ManagementHotelResult result = resumeHotelUseCase.execute(command);

    return ok("Mở bán lại khách sạn thành công.", result);
  }

  private static ResponseEntity<ApiResponse<ManagementHotelResponse>> ok(
      String message, ManagementHotelResult result) {
    ManagementHotelResponse response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success(message, response));
  }
}

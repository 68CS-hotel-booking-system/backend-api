package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.LockHotelRequest;
import com.team.hotelbooking.hotel.api.dto.request.UnlockHotelRequest;
import com.team.hotelbooking.hotel.api.dto.response.ManagementHotelResponse;
import com.team.hotelbooking.hotel.application.command.LockHotelCommand;
import com.team.hotelbooking.hotel.application.command.UnlockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.LockHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.UnlockHotelUseCase;
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

// HT-09: chỉ quản trị hệ thống khóa và gỡ khóa bán. Duyệt công khai lần đầu nằm ở
// PATCH /api/v1/management/hotels/{hotelId}/approve (AdminHotelController).
@RestController
@RequestMapping("/api/v1/admin/hotels")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminHotelStatusController {

  private final LockHotelUseCase lockHotelUseCase;
  private final UnlockHotelUseCase unlockHotelUseCase;

  @Operation(
      summary = "Khóa bán khách sạn",
      description =
          "Chỉ ADMIN, bắt buộc lý do. Khóa từ DRAFT, ACTIVE hoặc SUSPENDED; khách sạn đã LOCKED trả"
              + " 409. Chỉ chặn đơn mới, không hủy đơn đã có.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã khóa, trả trạng thái LOCKED"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Thiếu lý do hoặc lý do quá 500 ký tự",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Khách sạn đã bị khóa",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/lock")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> lockHotel(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody LockHotelRequest request,
      Authentication authentication) {
    LockHotelCommand command =
        new LockHotelCommand(CurrentActor.userId(authentication), hotelId, request.reason());

    ManagementHotelResult result = lockHotelUseCase.execute(command);

    return ok("Khóa bán khách sạn thành công.", result);
  }

  @Operation(
      summary = "Gỡ khóa bán khách sạn",
      description =
          "Chỉ ADMIN, bắt buộc lý do. Khách sạn về SUSPENDED để quản lý tự mở bán lại; nếu bị khóa"
              + " khi còn DRAFT thì về DRAFT. Khách sạn không bị khóa trả 409.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã gỡ khóa, trả trạng thái SUSPENDED hoặc DRAFT"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Thiếu lý do hoặc lý do quá 500 ký tự",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Khách sạn không ở trạng thái LOCKED",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/unlock")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> unlockHotel(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody UnlockHotelRequest request,
      Authentication authentication) {
    UnlockHotelCommand command =
        new UnlockHotelCommand(CurrentActor.userId(authentication), hotelId, request.reason());

    ManagementHotelResult result = unlockHotelUseCase.execute(command);

    return ok("Gỡ khóa bán khách sạn thành công.", result);
  }

  private static ResponseEntity<ApiResponse<ManagementHotelResponse>> ok(
      String message, ManagementHotelResult result) {
    ManagementHotelResponse response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success(message, response));
  }
}

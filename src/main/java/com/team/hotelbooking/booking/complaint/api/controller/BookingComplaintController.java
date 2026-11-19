package com.team.hotelbooking.booking.complaint.api.controller;

import com.team.hotelbooking.booking.complaint.api.dto.request.SubmitComplaintRequest;
import com.team.hotelbooking.booking.complaint.api.dto.response.ComplaintResponse;
import com.team.hotelbooking.booking.complaint.application.command.SubmitComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.SubmitComplaintUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/complaints")
@RequiredArgsConstructor
@Tag(name = "Bookings - Complaints", description = "Khách gửi và xem khiếu nại gắn đơn đặt phòng")
public class BookingComplaintController {

  private final SubmitComplaintUseCase submitComplaintUseCase;

  @Operation(
      summary = "[BK-21] Gửi khiếu nại cho đơn đặt phòng của mình",
      description =
          "Chỉ chủ đơn. Mọi trạng thái đơn trừ PENDING_PAYMENT. Mỗi đơn tối đa một khiếu nại đang"
              + " xử lý (OPEN/IN_REVIEW): gửi thêm trả 409 COMPLAINT_ALREADY_OPEN kèm"
              + " openComplaintId. Chưa hỗ trợ đính kèm bằng chứng. Không cam kết tự động bồi"
              + " thường hay hoàn tiền.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Đã tiếp nhận, trạng thái OPEN"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Loại khiếu nại sai hoặc nội dung không hợp lệ",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Đơn không tồn tại hoặc không phải của bạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Đơn đang chờ thanh toán hoặc đã có khiếu nại đang xử lý",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping
  public ResponseEntity<ApiResponse<ComplaintResponse>> submitComplaint(
      @PathVariable("bookingId") UUID bookingId,
      @Valid @RequestBody SubmitComplaintRequest request,
      Principal principal) {
    SubmitComplaintCommand command =
        new SubmitComplaintCommand(
            UUID.fromString(principal.getName()),
            bookingId,
            request.categoryValue(),
            request.content());

    ComplaintResult result = submitComplaintUseCase.execute(command);

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Gửi khiếu nại thành công.", ComplaintResponse.from(result)));
  }
}

package com.team.hotelbooking.booking.complaint.api.controller;

import com.team.hotelbooking.booking.complaint.api.dto.response.ComplaintResponse;
import com.team.hotelbooking.booking.complaint.application.command.GetMyComplaintQuery;
import com.team.hotelbooking.booking.complaint.application.command.ListMyComplaintsQuery;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.GetMyComplaintUseCase;
import com.team.hotelbooking.booking.complaint.application.usecase.ListMyComplaintsUseCase;
import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/complaints")
@RequiredArgsConstructor
@Tag(name = "Bookings - Complaints", description = "Khách gửi và xem khiếu nại gắn đơn đặt phòng")
public class MyComplaintController {

  private final ListMyComplaintsUseCase listMyComplaintsUseCase;
  private final GetMyComplaintUseCase getMyComplaintUseCase;
  private final PaginationSupport paginationSupport;

  @Operation(
      summary = "[BK-21] Danh sách khiếu nại của tôi",
      description = "Chỉ khiếu nại do tài khoản đang đăng nhập gửi, mới nhất trước.")
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ComplaintResponse>>> listMyComplaints(
      @Parameter(description = "Lọc theo trạng thái: OPEN, IN_REVIEW, RESOLVED, REJECTED")
          @RequestParam(name = "status", required = false)
          @Pattern(
              regexp = "OPEN|IN_REVIEW|RESOLVED|REJECTED",
              message = "Trạng thái chỉ nhận OPEN, IN_REVIEW, RESOLVED hoặc REJECTED.")
          String status,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListMyComplaintsQuery query =
        new ListMyComplaintsQuery(
            UUID.fromString(principal.getName()),
            status == null ? null : Complaint.Status.valueOf(status),
            pagination);

    PageResult<ComplaintResult> result = listMyComplaintsUseCase.execute(query);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách khiếu nại thành công.", ComplaintResponse.from(result)));
  }

  @Operation(
      summary = "[BK-21] Chi tiết khiếu nại của tôi",
      description = "Khiếu nại của người khác trả 404 như không tồn tại.")
  @GetMapping("/{complaintId}")
  public ResponseEntity<ApiResponse<ComplaintResponse>> getMyComplaint(
      @PathVariable("complaintId") UUID complaintId, Principal principal) {
    ComplaintResult result =
        getMyComplaintUseCase.execute(
            new GetMyComplaintQuery(UUID.fromString(principal.getName()), complaintId));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy chi tiết khiếu nại thành công.", ComplaintResponse.from(result)));
  }
}

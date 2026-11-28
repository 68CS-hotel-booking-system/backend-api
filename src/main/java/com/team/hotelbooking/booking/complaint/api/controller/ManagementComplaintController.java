package com.team.hotelbooking.booking.complaint.api.controller;

import com.team.hotelbooking.booking.complaint.api.dto.request.HandleComplaintRequest;
import com.team.hotelbooking.booking.complaint.api.dto.response.ManagedComplaintResponse;
import com.team.hotelbooking.booking.complaint.application.command.*;
import com.team.hotelbooking.booking.complaint.application.usecase.*;
import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/management/complaints")
@PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
@RequiredArgsConstructor
@Tag(
    name = "Bookings - Complaint Management",
    description = "Admin xử lý toàn hệ thống; owner xử lý khiếu nại tại khách sạn hiện thuộc mình")
public class ManagementComplaintController {
  private final ListManagedComplaintsUseCase list;
  private final GetManagedComplaintUseCase detail;
  private final HandleComplaintUseCase handle;
  private final PaginationSupport pagination;

  @GetMapping
  @Operation(summary = "Danh sách khiếu nại trong phạm vi quản lý, mới nhất trước")
  public ResponseEntity<ApiResponse<PageResponse<ManagedComplaintResponse>>> list(
      @RequestParam(name = "hotelId", required = false) UUID hotelId,
      @RequestParam(name = "status", required = false) Complaint.Status status,
      @RequestParam(name = "keyword", required = false)
          @Size(max = 100, message = "Từ khóa tối đa 100 ký tự.")
          String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Authentication authentication) {
    var result =
        list.execute(
            new ListManagedComplaintsQuery(
                CurrentActor.userId(authentication),
                hotelId,
                status,
                keyword,
                pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách khiếu nại thành công.", ManagedComplaintResponse.from(result)));
  }

  @GetMapping("/{complaintId}")
  @Operation(summary = "Chi tiết khiếu nại, liên hệ người đặt và lịch sử phản hồi")
  public ResponseEntity<ApiResponse<ManagedComplaintResponse>> detail(
      @PathVariable("complaintId") UUID complaintId, Authentication authentication) {
    var result =
        detail.execute(
            new GetManagedComplaintQuery(CurrentActor.userId(authentication), complaintId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy chi tiết khiếu nại thành công.", ManagedComplaintResponse.from(result)));
  }

  @PostMapping("/{complaintId}/actions")
  @Operation(
      summary = "Tiếp nhận xử lý, phản hồi, giải quyết hoặc từ chối khiếu nại",
      description =
          "Bắt buộc phản hồi cho khách và expectedVersion. OPEN chỉ chuyển IN_REVIEW; IN_REVIEW cho phép phản hồi tiếp hoặc đóng RESOLVED/REJECTED. Không tự hoàn tiền.")
  public ResponseEntity<ApiResponse<ManagedComplaintResponse>> handle(
      @PathVariable("complaintId") UUID complaintId,
      @Valid @RequestBody HandleComplaintRequest request,
      Authentication authentication) {
    var result =
        handle.execute(
            new HandleComplaintCommand(
                CurrentActor.userId(authentication),
                complaintId,
                request.status(),
                request.response(),
                request.expectedVersion()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Đã cập nhật xử lý khiếu nại.", ManagedComplaintResponse.from(result)));
  }
}

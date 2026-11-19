package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.ApproveHotelReviewRequest;
import com.team.hotelbooking.hotel.api.dto.request.RejectHotelReviewRequest;
import com.team.hotelbooking.hotel.api.dto.request.SubmitHotelReviewRequest;
import com.team.hotelbooking.hotel.api.dto.response.HotelReviewResponse;
import com.team.hotelbooking.hotel.api.dto.response.HotelReviewSummaryResponse;
import com.team.hotelbooking.hotel.application.command.ApproveHotelReviewCommand;
import com.team.hotelbooking.hotel.application.command.GetHotelReviewQuery;
import com.team.hotelbooking.hotel.application.command.ListHotelReviewsQuery;
import com.team.hotelbooking.hotel.application.command.ListManagementHotelReviewsQuery;
import com.team.hotelbooking.hotel.application.command.RejectHotelReviewCommand;
import com.team.hotelbooking.hotel.application.command.SubmitHotelReviewCommand;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListHotelReviewsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelReviewsUseCase;
import com.team.hotelbooking.hotel.application.usecase.RejectHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.SubmitHotelReviewUseCase;
import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HotelReviewController {
  private final SubmitHotelReviewUseCase submit;
  private final ListHotelReviewsUseCase list;
  private final ListManagementHotelReviewsUseCase listManagement;
  private final GetHotelReviewUseCase get;
  private final ApproveHotelReviewUseCase approve;
  private final RejectHotelReviewUseCase reject;
  private final PaginationSupport pagination;

  @PostMapping("/management/hotels/{hotelId}/review-requests")
  @PreAuthorize("hasRole('HOTEL_OWNER') and !hasRole('ADMIN')")
  @Operation(summary = "Owner gửi snapshot khách sạn DRAFT để xét duyệt")
  public ResponseEntity<ApiResponse<HotelReviewResponse>> submit(
      @PathVariable UUID hotelId,
      @Valid @RequestBody SubmitHotelReviewRequest request,
      Authentication authentication) {
    var result =
        submit.execute(
            new SubmitHotelReviewCommand(
                CurrentActor.userId(authentication), hotelId, request.expectedVersion()));
    return ResponseEntity.created(URI.create("/api/v1/hotel-review-requests/" + result.id()))
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Gửi hồ sơ khách sạn thành công.", HotelReviewResponse.from(result)));
  }

  @GetMapping("/admin/hotel-review-requests")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin liệt kê hồ sơ, mới nhất trước, lọc trạng thái và khách sạn")
  public ResponseEntity<ApiResponse<PageResponse<HotelReviewSummaryResponse>>> list(
      @RequestParam(required = false) HotelReviewRequest.Status status,
      @RequestParam(required = false) UUID hotelId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      Authentication authentication) {
    var result =
        list.execute(
            new ListHotelReviewsQuery(
                CurrentActor.userId(authentication),
                status,
                hotelId,
                pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách hồ sơ thành công.",
                PageResponse.from(result.map(HotelReviewSummaryResponse::from))));
  }

  @GetMapping("/hotel-review-requests/{requestId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  @Operation(summary = "Admin hoặc Owner của khách sạn đọc hồ sơ đã gửi")
  public ResponseEntity<ApiResponse<HotelReviewResponse>> get(
      @PathVariable UUID requestId, Authentication authentication) {
    var result =
        get.execute(new GetHotelReviewQuery(CurrentActor.userId(authentication), requestId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy hồ sơ thành công.", HotelReviewResponse.from(result)));
  }

  @GetMapping("/management/hotels/{hotelId}/review-requests")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  @Operation(summary = "Owner hoặc Admin xem lịch sử hồ sơ của khách sạn, mới nhất trước")
  public ResponseEntity<ApiResponse<PageResponse<HotelReviewSummaryResponse>>> listManagement(
      @PathVariable UUID hotelId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      Authentication authentication) {
    var result =
        listManagement.execute(
            new ListManagementHotelReviewsQuery(
                CurrentActor.userId(authentication), hotelId, pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy lịch sử hồ sơ thành công.",
                PageResponse.from(result.map(HotelReviewSummaryResponse::from))));
  }

  @PostMapping("/admin/hotel-review-requests/{requestId}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin duyệt hồ sơ PENDING và mở bán khách sạn trong cùng transaction")
  public ResponseEntity<ApiResponse<HotelReviewResponse>> approve(
      @PathVariable UUID requestId,
      @Valid @RequestBody ApproveHotelReviewRequest request,
      Authentication authentication) {
    var result =
        approve.execute(
            new ApproveHotelReviewCommand(
                CurrentActor.userId(authentication), requestId, request.expectedVersion()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Duyệt hồ sơ thành công.", HotelReviewResponse.from(result)));
  }

  @PostMapping("/admin/hotel-review-requests/{requestId}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Admin từ chối hồ sơ PENDING, bắt buộc lý do")
  public ResponseEntity<ApiResponse<HotelReviewResponse>> reject(
      @PathVariable UUID requestId,
      @Valid @RequestBody RejectHotelReviewRequest request,
      Authentication authentication) {
    var result =
        reject.execute(
            new RejectHotelReviewCommand(
                CurrentActor.userId(authentication),
                requestId,
                request.expectedVersion(),
                request.reason()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Từ chối hồ sơ thành công.", HotelReviewResponse.from(result)));
  }
}

package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.response.BookingDetailResponse;
import com.team.hotelbooking.booking.api.dto.response.BookingSummaryResponse;
import com.team.hotelbooking.booking.application.command.GetManagementBookingDetailQuery;
import com.team.hotelbooking.booking.application.command.ListManagementBookingsQuery;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.booking.application.usecase.GetManagementBookingDetailUseCase;
import com.team.hotelbooking.booking.application.usecase.ListManagementBookingsUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/bookings")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
@RequiredArgsConstructor
@Tag(
    name = "Bookings - Management",
    description = "API quản lý danh sách và chi tiết đơn đặt phòng theo khách sạn")
public class ManagementBookingController {

  private final ListManagementBookingsUseCase listManagementBookingsUseCase;
  private final GetManagementBookingDetailUseCase getManagementBookingDetailUseCase;
  private final PaginationSupport paginationSupport;

  @Operation(
      summary = "[BK04] Xem danh sách đơn đặt phòng của khách sạn",
      description = "Lấy danh sách phân trang các đơn đặt phòng thuộc khách sạn quản lý.")
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> listHotelBookings(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListManagementBookingsQuery query =
        new ListManagementBookingsQuery(
            UUID.fromString(principal.getName()), hotelId, status, pagination);

    PageResult<BookingSummaryResult> result = listManagementBookingsUseCase.execute(query);

    PageResponse<BookingSummaryResponse> response = BookingSummaryResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách đơn đặt phòng khách sạn thành công.", response));
  }

  @Operation(
      summary = "[BK05] Xem chi tiết đơn đặt phòng của khách sạn",
      description = "Xem chi tiết đơn đặt phòng thuộc khách sạn quản lý.")
  @GetMapping("/{bookingId}")
  public ResponseEntity<ApiResponse<BookingDetailResponse>> getHotelBookingDetail(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("bookingId") UUID bookingId,
      Principal principal) {
    GetManagementBookingDetailQuery query =
        new GetManagementBookingDetailQuery(
            UUID.fromString(principal.getName()), hotelId, bookingId);

    BookingDetailResult result = getManagementBookingDetailUseCase.execute(query);

    BookingDetailResponse response = BookingDetailResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy chi tiết đơn đặt phòng khách sạn thành công.", response));
  }
}

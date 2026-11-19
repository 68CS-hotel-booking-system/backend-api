package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.request.CreateBookingRequest;
import com.team.hotelbooking.booking.api.dto.response.BookingDetailResponse;
import com.team.hotelbooking.booking.api.dto.response.BookingSummaryResponse;
import com.team.hotelbooking.booking.application.command.GetCustomerBookingDetailQuery;
import com.team.hotelbooking.booking.application.command.ListCustomerBookingsQuery;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.application.usecase.GetCustomerBookingDetailUseCase;
import com.team.hotelbooking.booking.application.usecase.ListCustomerBookingsUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings - Customer", description = "API tạo, xem danh sách và chi tiết đơn đặt phòng")
public class BookingController {

  private final CreateBookingUseCase createBookingUseCase;
  private final ListCustomerBookingsUseCase listCustomerBookingsUseCase;
  private final GetCustomerBookingDetailUseCase getCustomerBookingDetailUseCase;
  private final PaginationSupport paginationSupport;

  @Operation(
      summary = "[BK01] Tạo đơn đặt phòng, lưu giá/phụ thu và thông tin người đặt/người ở",
      description =
          "Tính lại và lưu snapshot ngày/đêm, giờ nhận/trả, tiền phòng từng đêm, phụ thu, thông tin người đặt/người ở và bàn giao kết quả cho thanh toán.")
  @PostMapping
  public ResponseEntity<ApiResponse<BookingDetailResponse>> createBooking(
      @Valid @RequestBody CreateBookingRequest request, Principal principal) {
    UUID customerId = UUID.fromString(principal.getName());

    BookingDetailResult result = createBookingUseCase.execute(request.toCommand(customerId));

    BookingDetailResponse response = BookingDetailResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Tạo đơn đặt phòng thành công.", response));
  }

  @Operation(
      summary = "[BK02] Xem danh sách đơn đặt phòng của khách hàng",
      description = "Lấy danh sách phân trang các đơn đặt phòng của tài khoản đang đăng nhập.")
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> listBookings(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListCustomerBookingsQuery query =
        new ListCustomerBookingsQuery(UUID.fromString(principal.getName()), status, pagination);

    PageResult<BookingSummaryResult> result = listCustomerBookingsUseCase.execute(query);

    PageResponse<BookingSummaryResponse> response = BookingSummaryResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách đơn đặt phòng thành công.", response));
  }

  @Operation(
      summary = "[BK03] Xem chi tiết đơn đặt phòng của khách hàng",
      description =
          "Xem chi tiết ngày/đêm, giờ nhận/trả, giá từng đêm, phụ thu, người đặt/người ở và dữ liệu bàn giao thanh toán.")
  @GetMapping("/{bookingId}")
  public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(
      @PathVariable("bookingId") UUID bookingId, Principal principal) {
    GetCustomerBookingDetailQuery query =
        new GetCustomerBookingDetailQuery(UUID.fromString(principal.getName()), bookingId);

    BookingDetailResult result = getCustomerBookingDetailUseCase.execute(query);

    BookingDetailResponse response = BookingDetailResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy chi tiết đơn đặt phòng thành công.", response));
  }
}

package com.team.hotelbooking.booking.review.api.controller;

import com.team.hotelbooking.booking.review.api.dto.request.*;
import com.team.hotelbooking.booking.review.api.dto.response.*;
import com.team.hotelbooking.booking.review.application.command.*;
import com.team.hotelbooking.booking.review.application.usecase.*;
import com.team.hotelbooking.shared.web.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GuestReviewController {
  private final SubmitGuestReviewUseCase submit;
  private final GetBookingGuestReviewUseCase get;
  private final ListGuestReviewsUseCase list;
  private final ReplyGuestReviewUseCase reply;
  private final PaginationSupport pagination;

  @GetMapping("/api/v1/bookings/{bookingId}/review")
  public ResponseEntity<ApiResponse<BookingGuestReviewResponse>> get(
      @PathVariable("bookingId") UUID bookingId, Principal principal) {
    var result =
        get.execute(
            new GetBookingGuestReviewQuery(UUID.fromString(principal.getName()), bookingId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Thông tin đánh giá của đơn.", BookingGuestReviewResponse.from(result)));
  }

  @PostMapping("/api/v1/bookings/{bookingId}/review")
  public ResponseEntity<ApiResponse<GuestReviewResponse>> submit(
      @PathVariable("bookingId") UUID bookingId,
      @Valid @RequestBody SubmitGuestReviewRequest body,
      Principal principal) {
    var result =
        submit.execute(
            new SubmitGuestReviewCommand(
                UUID.fromString(principal.getName()),
                bookingId,
                body.rating().intValueExact(),
                body.content()));
    return ResponseEntity.created(URI.create("/api/v1/bookings/" + bookingId + "/review"))
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Đã gửi đánh giá sau lưu trú.", GuestReviewResponse.from(result)));
  }

  @GetMapping("/api/v1/hotels/{hotelId}/guest-reviews")
  public ResponseEntity<ApiResponse<PublicGuestReviewPageResponse>> publicList(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "rating", required = false) @Min(1) @Max(5) Integer rating,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    var result =
        list.execute(
            new ListGuestReviewsQuery(
                null, hotelId, false, rating, null, pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-cache")
        .body(
            ApiResponse.success(
                "Đánh giá từ khách lưu trú.", PublicGuestReviewPageResponse.from(result)));
  }

  @GetMapping("/api/v1/management/hotels/{hotelId}/guest-reviews")
  @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<ManagedGuestReviewPageResponse>> managementList(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "rating", required = false) @Min(1) @Max(5) Integer rating,
      @RequestParam(name = "replied", required = false) Boolean replied,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    var result =
        list.execute(
            new ListGuestReviewsQuery(
                UUID.fromString(principal.getName()),
                hotelId,
                true,
                rating,
                replied,
                pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Danh sách đánh giá của khách sạn.", ManagedGuestReviewPageResponse.from(result)));
  }

  @PutMapping("/api/v1/management/hotels/{hotelId}/guest-reviews/{reviewId}/reply")
  @PreAuthorize("hasRole('HOTEL_OWNER') and !hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<GuestReviewResponse>> reply(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("reviewId") UUID reviewId,
      @Valid @RequestBody ReplyGuestReviewRequest body,
      Principal principal) {
    var result =
        reply.execute(
            new ReplyGuestReviewCommand(
                UUID.fromString(principal.getName()),
                hotelId,
                reviewId,
                body.content(),
                body.expectedVersion()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Đã lưu phản hồi của khách sạn.", GuestReviewResponse.from(result)));
  }
}

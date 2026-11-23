package com.team.hotelbooking.booking.api.dto.request;

import com.team.hotelbooking.booking.api.dto.request.CreateBookingRequest.BookingItemRequest;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.StayQuoteCriteria;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.StayQuoteItemCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record QuoteVoucherRequest(
    @NotBlank(message = "Mã voucher không được để trống.") String voucherCode,
    @NotNull(message = "Mã khách sạn không được để trống.") UUID hotelId,
    @NotNull(message = "Ngày nhận phòng không được để trống.") LocalDate checkIn,
    @NotNull(message = "Ngày trả phòng không được để trống.") LocalDate checkOut,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    @NotEmpty(message = "Danh sách hạng mục đặt phòng không được để trống.")
        List<@NotNull @Valid BookingItemRequest> items) {

  public StayQuoteCriteria toCriteria() {
    return new StayQuoteCriteria(
        hotelId,
        checkIn,
        checkOut,
        expectedCheckInTime,
        expectedCheckOutTime,
        items.stream()
            .map(
                i ->
                    new StayQuoteItemCriteria(
                        i.roomTypeId(), i.quantity(), i.adults(), i.children()))
            .toList());
  }
}

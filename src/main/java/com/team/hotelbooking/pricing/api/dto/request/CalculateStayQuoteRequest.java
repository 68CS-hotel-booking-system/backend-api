package com.team.hotelbooking.pricing.api.dto.request;

import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery.QuoteItemInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CalculateStayQuoteRequest(
    @NotNull(message = "Ngày nhận phòng không được để trống.") LocalDate checkIn,
    @NotNull(message = "Ngày trả phòng không được để trống.") LocalDate checkOut,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    @NotEmpty(message = "Danh sách hạng mục phòng không được để trống.")
        List<@Valid QuoteItemRequest> items) {

  public record QuoteItemRequest(
      @NotNull(message = "Mã loại phòng không được để trống.") UUID roomTypeId,
      @Min(value = 1, message = "Số lượng phòng phải lớn hơn hoặc bằng 1.") int quantity,
      @Min(value = 1, message = "Số lượng người lớn phải lớn hơn hoặc bằng 1.") int adults,
      @Min(value = 0, message = "Số lượng trẻ em không được âm.") int children) {

    public QuoteItemInput toInput() {
      return new QuoteItemInput(roomTypeId, quantity, adults, children);
    }
  }

  public CalculateStayQuoteQuery toQuery(UUID hotelId) {
    List<QuoteItemInput> inputs =
        items == null ? List.of() : items.stream().map(QuoteItemRequest::toInput).toList();
    return new CalculateStayQuoteQuery(
        hotelId, checkIn, checkOut, expectedCheckInTime, expectedCheckOutTime, inputs);
  }
}

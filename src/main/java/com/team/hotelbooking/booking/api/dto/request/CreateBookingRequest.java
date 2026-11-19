package com.team.hotelbooking.booking.api.dto.request;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.BookingItemInput;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
    @NotNull(message = "Mã khách sạn không được để trống.") UUID hotelId,
    @NotNull(message = "Ngày nhận phòng không được để trống.") LocalDate checkIn,
    @NotNull(message = "Ngày trả phòng không được để trống.") LocalDate checkOut,
    LocalTime expectedCheckInTime,
    LocalTime expectedCheckOutTime,
    @NotNull(message = "Thông tin người đặt không được để trống.") @Valid ContactRequest booker,
    @Valid ContactRequest guest,
    @NotEmpty(message = "Danh sách hạng mục đặt phòng không được để trống.")
        List<@Valid BookingItemRequest> items,
    @Min(value = 1, message = "Tổng báo giá xác nhận phải lớn hơn 0.") Long quotedTotalAmount) {

  public record ContactRequest(
      @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự.") String fullName,
      @Size(max = 255, message = "Email không được vượt quá 255 ký tự.") String email,
      @NotBlank(message = "Số điện thoại liên hệ không được để trống.")
          @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
          String phone) {

    public ContactInput toInput() {
      return new ContactInput(fullName, email, phone);
    }
  }

  public record BookingItemRequest(
      @NotNull(message = "Mã loại phòng không được để trống.") UUID roomTypeId,
      @Min(value = 1, message = "Số lượng phòng phải từ 1 trở lên.") int quantity,
      @Min(value = 1, message = "Số lượng người lớn phải từ 1 trở lên.") int adults,
      @Min(value = 0, message = "Số lượng trẻ em không được âm.") int children) {

    public BookingItemInput toInput() {
      return new BookingItemInput(roomTypeId, quantity, adults, children);
    }
  }

  public CreateBookingCommand toCommand(UUID customerId) {
    List<BookingItemInput> itemInputs =
        items == null ? List.of() : items.stream().map(BookingItemRequest::toInput).toList();
    return new CreateBookingCommand(
        customerId,
        hotelId,
        checkIn,
        checkOut,
        expectedCheckInTime,
        expectedCheckOutTime,
        booker == null ? null : booker.toInput(),
        guest == null ? null : guest.toInput(),
        itemInputs,
        quotedTotalAmount);
  }
}

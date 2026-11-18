package com.team.hotelbooking.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingCheckoutDirectory {

  Optional<BookingCheckoutSnapshot> findById(UUID bookingId);

  /** Khóa đơn trong transaction của bên gọi, tuần tự hóa tạo thanh toán và IPN. */
  Optional<BookingCheckoutSnapshot> lockById(UUID bookingId);

  /** Chỉ xác nhận đơn chờ thanh toán khi giữ phòng vẫn còn hiệu lực. */
  boolean confirmPayment(UUID bookingId, Instant now);

  /** Khóa đơn trong transaction của bên gọi và trả false nếu đơn đã hủy. */
  boolean lockForStayOperation(UUID bookingId);

  boolean updateStayStatus(UUID bookingId, String status, Instant updatedAt);

  record BookingCheckoutSnapshot(
      UUID bookingId,
      String bookingCode,
      UUID customerId,
      UUID hotelId,
      ContactSnapshot booker,
      ContactSnapshot guest,
      LocalDate checkIn,
      LocalDate checkOut,
      int days,
      int nights,
      LocalTime standardCheckInTime,
      LocalTime standardCheckOutTime,
      LocalTime expectedCheckInTime,
      LocalTime expectedCheckOutTime,
      int adults,
      int children,
      String status,
      String currency,
      long roomSubtotalAmount,
      long earlyCheckInSurchargeAmount,
      long lateCheckOutSurchargeAmount,
      long extraGuestSurchargeAmount,
      long surchargeAmount,
      long subtotalAmount,
      long discountAmount,
      long totalAmount,
      Instant expiresAt,
      List<ItemSnapshot> items) {

    public record ContactSnapshot(String fullName, String email, String phone) {}

    public record ItemSnapshot(
        UUID id,
        UUID roomTypeId,
        String roomTypeName,
        int quantity,
        int adults,
        int children,
        int extraGuests,
        long roomSubtotalAmount,
        long surchargeAmount,
        long subtotalAmount,
        List<NightlyPriceSnapshot> nightlyPrices) {}

    public record NightlyPriceSnapshot(LocalDate stayDate, long unitPrice) {}
  }
}

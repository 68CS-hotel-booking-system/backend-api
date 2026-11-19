package com.team.hotelbooking.payment.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.ContactSnapshot;
import com.team.hotelbooking.payment.application.port.PaymentBookingPort.BookingPaymentSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Adapter bàn giao dữ liệu tính toán đơn đặt phòng sang module thanh toán")
class PaymentBookingAdapterTests {

  @Mock private BookingCheckoutDirectory bookingCheckoutDirectory;

  @Test
  @DisplayName(
      "Ánh xạ đầy đủ ngày/đêm, giờ nhận/trả, người đặt/người ở, tiền phòng, phụ thu và tổng phải thu cho thanh toán")
  void shouldMapBookingCheckoutSnapshotForPayment() {
    // Given
    UUID bookingId = UUID.fromString("50000000-0000-0000-0000-000000000005");
    UUID customerId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID hotelId = UUID.fromString("30000000-0000-0000-0000-000000000003");
    Instant expiresAt = Instant.parse("2026-11-09T02:15:00Z");

    BookingCheckoutSnapshot checkoutSnapshot =
        new BookingCheckoutSnapshot(
            bookingId,
            "BK-20261109-ABCD",
            customerId,
            hotelId,
            new ContactSnapshot("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
            new ContactSnapshot("Trần Thị Ở", "guest@example.com", "0987654321"),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12),
            3,
            2,
            LocalTime.of(14, 0),
            LocalTime.of(12, 0),
            LocalTime.of(8, 0),
            LocalTime.of(12, 0),
            2,
            1,
            "PENDING_PAYMENT",
            "VND",
            2_000_000L,
            500_000L,
            0L,
            200_000L,
            700_000L,
            2_700_000L,
            0L,
            2_700_000L,
            expiresAt,
            List.of());

    given(bookingCheckoutDirectory.findById(bookingId)).willReturn(Optional.of(checkoutSnapshot));
    PaymentBookingAdapter adapter = new PaymentBookingAdapter(bookingCheckoutDirectory);

    // When
    Optional<BookingPaymentSnapshot> result = adapter.findBookingForPayment(bookingId);

    // Then
    assertTrue(result.isPresent());
    BookingPaymentSnapshot snapshot = result.get();
    assertEquals(bookingId, snapshot.bookingId());
    assertEquals("BK-20261109-ABCD", snapshot.bookingCode());
    assertEquals("Nguyễn Văn Đặt", snapshot.bookerName());
    assertEquals("Trần Thị Ở", snapshot.guestName());
    assertEquals(3, snapshot.days());
    assertEquals(2, snapshot.nights());
    assertEquals(2_000_000L, snapshot.roomSubtotalAmount());
    assertEquals(700_000L, snapshot.surchargeAmount());
    assertEquals(2_700_000L, snapshot.subtotalAmount());
    assertEquals(2_700_000L, snapshot.payableAmount());
    assertEquals("VND", snapshot.currency());
    assertEquals("PENDING_PAYMENT", snapshot.status());
    assertEquals(expiresAt, snapshot.holdExpiresAt());
  }
}

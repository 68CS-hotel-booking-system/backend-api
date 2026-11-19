package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.ContactSnapshot;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.ItemSnapshot;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.NightlyPriceSnapshot;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class BookingCheckoutDirectoryAdapter implements BookingCheckoutDirectory {

  private final BookingRepository bookings;
  private final BookingInventoryPort inventory;

  public BookingCheckoutDirectoryAdapter(
      BookingRepository bookings, BookingInventoryPort inventory) {
    this.bookings = bookings;
    this.inventory = inventory;
  }

  @Override
  public Optional<BookingCheckoutSnapshot> lockById(UUID bookingId) {
    return bookings.lockById(bookingId).map(this::snapshot);
  }

  @Override
  public boolean confirmPayment(UUID bookingId, Instant now) {
    var booking = bookings.lockById(bookingId);
    if (booking.isEmpty()
        || booking.get().status() != Booking.Status.PENDING_PAYMENT
        || booking.get().expiresAt() == null
        || !booking.get().expiresAt().isAfter(now)) return false;
    if (!inventory.confirmIfHeld(booking.get())) return false;
    if (!bookings.confirmPayment(bookingId, now)) {
      throw new IllegalStateException("Không thể xác nhận đơn đã khóa.");
    }
    return true;
  }

  @Override
  public boolean updateStayStatus(UUID bookingId, String status, Instant updatedAt) {
    if (bookingId == null || status == null) {
      return false;
    }
    return bookings.updateStatus(bookingId, Booking.Status.valueOf(status), updatedAt);
  }

  @Override
  public Optional<BookingCheckoutSnapshot> findById(UUID bookingId) {
    return bookings.findById(bookingId).map(this::snapshot);
  }

  private BookingCheckoutSnapshot snapshot(Booking booking) {
    return new BookingCheckoutSnapshot(
        booking.id(),
        booking.bookingCode(),
        booking.customerId(),
        booking.hotelId(),
        new ContactSnapshot(
            booking.booker().fullName(), booking.booker().email(), booking.booker().phone()),
        new ContactSnapshot(
            booking.guest().fullName(), booking.guest().email(), booking.guest().phone()),
        booking.checkIn(),
        booking.checkOut(),
        booking.days(),
        booking.nights(),
        booking.standardCheckInTime(),
        booking.standardCheckOutTime(),
        booking.expectedCheckInTime(),
        booking.expectedCheckOutTime(),
        booking.adults(),
        booking.children(),
        booking.status().name(),
        booking.currency(),
        booking.roomSubtotalAmount(),
        booking.earlyCheckInSurchargeAmount(),
        booking.lateCheckOutSurchargeAmount(),
        booking.extraGuestSurchargeAmount(),
        booking.surchargeAmount(),
        booking.subtotalAmount(),
        booking.discountAmount(),
        booking.totalAmount(),
        booking.expiresAt(),
        booking.items().stream()
            .map(
                item ->
                    new ItemSnapshot(
                        item.id(),
                        item.roomTypeId(),
                        item.roomTypeNameSnapshot(),
                        item.quantity(),
                        item.adults(),
                        item.children(),
                        item.extraGuests(),
                        item.roomSubtotalAmount(),
                        item.surchargeAmount(),
                        item.subtotalAmount(),
                        item.nightlyPrices().stream()
                            .map(np -> new NightlyPriceSnapshot(np.stayDate(), np.unitPrice()))
                            .toList()))
            .toList());
  }
}

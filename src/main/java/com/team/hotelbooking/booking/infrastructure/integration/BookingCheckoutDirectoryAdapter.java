package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.ContactSnapshot;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.ItemSnapshot;
import com.team.hotelbooking.booking.BookingCheckoutDirectory.BookingCheckoutSnapshot.NightlyPriceSnapshot;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import java.util.Optional;
import java.util.UUID;

public class BookingCheckoutDirectoryAdapter implements BookingCheckoutDirectory {

  private final BookingRepository bookings;

  public BookingCheckoutDirectoryAdapter(BookingRepository bookings) {
    this.bookings = bookings;
  }

  @Override
  public Optional<BookingCheckoutSnapshot> findById(UUID bookingId) {
    return bookings
        .findById(bookingId)
        .map(
            booking ->
                new BookingCheckoutSnapshot(
                    booking.id(),
                    booking.bookingCode(),
                    booking.customerId(),
                    booking.hotelId(),
                    new ContactSnapshot(
                        booking.booker().fullName(),
                        booking.booker().email(),
                        booking.booker().phone()),
                    new ContactSnapshot(
                        booking.guest().fullName(),
                        booking.guest().email(),
                        booking.guest().phone()),
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
                                        .map(
                                            np ->
                                                new NightlyPriceSnapshot(
                                                    np.stayDate(), np.unitPrice()))
                                        .toList()))
                        .toList()));
  }
}

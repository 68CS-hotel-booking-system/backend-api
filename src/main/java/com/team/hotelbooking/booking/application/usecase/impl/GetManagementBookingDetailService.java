package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.GetManagementBookingDetailQuery;
import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.usecase.GetManagementBookingDetailUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;

public final class GetManagementBookingDetailService implements GetManagementBookingDetailUseCase {

  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;
  private final BookingHotelPort hotels;

  public GetManagementBookingDetailService(
      BookingRepository bookings, BookingIdentityPort accounts, BookingHotelPort hotels) {
    this.bookings = bookings;
    this.accounts = accounts;
    this.hotels = hotels;
  }

  @Override
  public BookingDetailResult execute(GetManagementBookingDetailQuery query) {
    if (query == null
        || query.actorId() == null
        || query.hotelId() == null
        || query.bookingId() == null) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    ListManagementBookingsService.verifyHotelManagementAccess(
        query.actorId(), query.hotelId(), accounts, hotels);

    Booking booking =
        bookings
            .findByIdAndHotelId(query.bookingId(), query.hotelId())
            .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));

    return BookingDetailResult.from(booking);
  }
}

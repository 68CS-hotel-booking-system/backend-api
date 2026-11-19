package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.GetCustomerBookingDetailQuery;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort.AccountSnapshot;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.usecase.GetCustomerBookingDetailUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;

public final class GetCustomerBookingDetailService implements GetCustomerBookingDetailUseCase {

  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;

  public GetCustomerBookingDetailService(BookingRepository bookings, BookingIdentityPort accounts) {
    this.bookings = bookings;
    this.accounts = accounts;
  }

  @Override
  public BookingDetailResult execute(GetCustomerBookingDetailQuery query) {
    if (query == null || query.customerId() == null || query.bookingId() == null) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    AccountSnapshot customer =
        accounts
            .findById(query.customerId())
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));

    if (!customer.active()) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    Booking booking =
        bookings
            .findByIdAndCustomerId(query.bookingId(), customer.id())
            .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));

    return BookingDetailResult.from(booking);
  }
}

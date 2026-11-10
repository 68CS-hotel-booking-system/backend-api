package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.ListCustomerBookingsQuery;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort.AccountSnapshot;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.booking.application.usecase.ListCustomerBookingsUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Locale;

public final class ListCustomerBookingsService implements ListCustomerBookingsUseCase {

  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;
  private final int maxPageSize;

  public ListCustomerBookingsService(
      BookingRepository bookings, BookingIdentityPort accounts, int maxPageSize) {
    if (maxPageSize < 1) {
      throw new IllegalArgumentException("Max page size must be at least 1.");
    }
    this.bookings = bookings;
    this.accounts = accounts;
    this.maxPageSize = maxPageSize;
  }

  @Override
  public PageResult<BookingSummaryResult> execute(ListCustomerBookingsQuery query) {
    if (query == null || query.customerId() == null || query.pagination() == null) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    PageQuery pagination = query.pagination();
    if (pagination.size() > maxPageSize) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    AccountSnapshot customer =
        accounts
            .findById(query.customerId())
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));

    if (!customer.active()) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    Booking.Status statusFilter = parseStatusFilter(query.status());

    return bookings
        .findByCustomerId(customer.id(), statusFilter, pagination)
        .map(BookingSummaryResult::from);
  }

  static Booking.Status parseStatusFilter(String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return null;
    }
    try {
      return Booking.Status.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new BookingException(
          BookingErrorCode.VALIDATION_FAILED, "Trạng thái đơn đặt phòng không hợp lệ.");
    }
  }
}

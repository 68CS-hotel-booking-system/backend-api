package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.ListManagementBookingsQuery;
import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.booking.application.port.BookingHotelPort.HotelSnapshot;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort.AccountSnapshot;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.booking.application.usecase.ListManagementBookingsUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;

public final class ListManagementBookingsService implements ListManagementBookingsUseCase {

  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;
  private final BookingHotelPort hotels;
  private final int maxPageSize;

  public ListManagementBookingsService(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingHotelPort hotels,
      int maxPageSize) {
    if (maxPageSize < 1) {
      throw new IllegalArgumentException("Max page size must be at least 1.");
    }
    this.bookings = bookings;
    this.accounts = accounts;
    this.hotels = hotels;
    this.maxPageSize = maxPageSize;
  }

  @Override
  public PageResult<BookingSummaryResult> execute(ListManagementBookingsQuery query) {
    if (query == null
        || query.actorId() == null
        || query.hotelId() == null
        || query.pagination() == null) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    PageQuery pagination = query.pagination();
    if (pagination.size() > maxPageSize) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    verifyHotelManagementAccess(query.actorId(), query.hotelId(), accounts, hotels);

    Booking.Status statusFilter = ListCustomerBookingsService.parseStatusFilter(query.status());

    return bookings
        .findByHotelId(query.hotelId(), statusFilter, pagination)
        .map(BookingSummaryResult::from);
  }

  static void verifyHotelManagementAccess(
      java.util.UUID actorId,
      java.util.UUID hotelId,
      BookingIdentityPort accounts,
      BookingHotelPort hotels) {
    AccountSnapshot actor =
        accounts
            .findById(actorId)
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));

    if (!actor.active() || !actor.emailVerified()) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    boolean isAdmin = actor.roles().contains("ADMIN");
    boolean isOwnerRole = actor.roles().contains("HOTEL_OWNER");
    if (!isAdmin && !isOwnerRole) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    HotelSnapshot hotel =
        hotels
            .findById(hotelId)
            .orElseThrow(() -> new BookingException(BookingErrorCode.HOTEL_NOT_FOUND));

    if (!isAdmin && !actor.id().equals(hotel.ownerId())) {
      throw new BookingException(BookingErrorCode.HOTEL_NOT_FOUND);
    }
  }
}

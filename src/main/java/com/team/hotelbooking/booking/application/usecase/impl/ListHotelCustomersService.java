package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.ListHotelCustomersQuery;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.HotelCustomerPort;
import com.team.hotelbooking.booking.application.result.HotelCustomerResult;
import com.team.hotelbooking.booking.application.usecase.ListHotelCustomersUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.shared.pagination.PageResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListHotelCustomersService implements ListHotelCustomersUseCase {
  private final BookingIdentityPort accounts;
  private final HotelCustomerPort customers;
  private final int maxSize;

  @Override
  public PageResult<HotelCustomerResult> execute(ListHotelCustomersQuery query) {
    if (query == null
        || query.actorId() == null
        || query.pagination() == null
        || query.pagination().page() < 0
        || query.pagination().size() < 1
        || query.pagination().size() > maxSize
        || (query.keyword() != null && query.keyword().length() > 100))
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    var actor =
        accounts
            .findById(query.actorId())
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));
    if (!actor.active() || !actor.emailVerified() || !actor.roles().contains("HOTEL_OWNER"))
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    return customers.findCustomers(
        actor.id(), query.keyword() == null ? "" : query.keyword().trim(), query.pagination());
  }
}

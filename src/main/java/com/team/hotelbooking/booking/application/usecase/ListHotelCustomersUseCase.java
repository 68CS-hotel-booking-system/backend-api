package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.ListHotelCustomersQuery;
import com.team.hotelbooking.booking.application.result.HotelCustomerResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListHotelCustomersUseCase {
  PageResult<HotelCustomerResult> execute(ListHotelCustomersQuery query);
}

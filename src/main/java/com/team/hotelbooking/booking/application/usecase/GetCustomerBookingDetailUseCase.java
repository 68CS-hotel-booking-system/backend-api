package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.GetCustomerBookingDetailQuery;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;

public interface GetCustomerBookingDetailUseCase {

  BookingDetailResult execute(GetCustomerBookingDetailQuery query);
}

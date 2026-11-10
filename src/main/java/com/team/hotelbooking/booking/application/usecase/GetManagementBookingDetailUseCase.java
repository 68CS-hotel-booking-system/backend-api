package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.GetManagementBookingDetailQuery;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;

public interface GetManagementBookingDetailUseCase {

  BookingDetailResult execute(GetManagementBookingDetailQuery query);
}

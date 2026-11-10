package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.ListCustomerBookingsQuery;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListCustomerBookingsUseCase {

  PageResult<BookingSummaryResult> execute(ListCustomerBookingsQuery query);
}

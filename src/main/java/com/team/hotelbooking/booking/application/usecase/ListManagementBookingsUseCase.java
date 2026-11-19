package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.ListManagementBookingsQuery;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListManagementBookingsUseCase {

  PageResult<BookingSummaryResult> execute(ListManagementBookingsQuery query);
}

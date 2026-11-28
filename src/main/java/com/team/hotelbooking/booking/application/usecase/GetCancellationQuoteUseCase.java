package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.CancelBookingCommand;
import com.team.hotelbooking.booking.application.result.CancellationQuoteResult;

public interface GetCancellationQuoteUseCase {
  CancellationQuoteResult quote(CancelBookingCommand query);
}

package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.SaveVoucherCommand;
import com.team.hotelbooking.booking.application.result.SaveVoucherResult;

public interface SaveVoucherUseCase {
  SaveVoucherResult execute(SaveVoucherCommand command);
}

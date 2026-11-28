package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.RenewContractCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;

public interface RenewContractUseCase {
  HotelContractResult execute(RenewContractCommand command);
}

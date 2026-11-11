package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;

public interface ActivateContractUseCase {

  HotelContractResult execute(ChangeContractStatusCommand command);
}

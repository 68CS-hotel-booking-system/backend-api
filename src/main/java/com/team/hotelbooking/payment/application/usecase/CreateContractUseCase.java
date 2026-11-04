package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;

public interface CreateContractUseCase {
  HotelContractResult execute(CreateContractCommand command);
}

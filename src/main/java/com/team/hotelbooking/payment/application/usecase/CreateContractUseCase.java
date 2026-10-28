package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.domain.model.HotelContract;

public interface CreateContractUseCase {
  HotelContract execute(CreateContractCommand command);
}

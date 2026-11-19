package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.UpdateFinancialAccessCommand;
import com.team.hotelbooking.payment.application.result.FinancialAccessResult;

public interface UpdateFinancialAccessUseCase {
  FinancialAccessResult execute(UpdateFinancialAccessCommand command);
}

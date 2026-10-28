package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.UpdateFinancialAccessCommand;
import com.team.hotelbooking.payment.domain.model.FinancialAccess;

public interface UpdateFinancialAccessUseCase {
  FinancialAccess execute(UpdateFinancialAccessCommand command);
}

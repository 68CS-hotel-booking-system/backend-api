package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.GetFinancialAccessQuery;
import com.team.hotelbooking.payment.application.result.FinancialAccessDetailResult;

public interface GetFinancialAccessUseCase {
  FinancialAccessDetailResult execute(GetFinancialAccessQuery query);
}

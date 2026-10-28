package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.GetFinancialAccessQuery;
import com.team.hotelbooking.payment.domain.model.FinancialAccessDetail;

public interface GetFinancialAccessUseCase {
  FinancialAccessDetail execute(GetFinancialAccessQuery query);
}

package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.GetFinanceDashboardQuery;
import com.team.hotelbooking.payment.application.result.FinanceDashboardResult;

public interface GetFinanceDashboardUseCase {
  FinanceDashboardResult execute(GetFinanceDashboardQuery query);
}

package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.ListRefundsQuery;
import com.team.hotelbooking.payment.application.result.RefundResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListRefundsUseCase {
  PageResult<RefundResult> execute(ListRefundsQuery query);
}

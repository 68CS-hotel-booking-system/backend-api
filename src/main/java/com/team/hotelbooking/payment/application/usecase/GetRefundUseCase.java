package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.GetRefundQuery;
import com.team.hotelbooking.payment.application.result.RefundResult;

public interface GetRefundUseCase {
  RefundResult execute(GetRefundQuery query);
}

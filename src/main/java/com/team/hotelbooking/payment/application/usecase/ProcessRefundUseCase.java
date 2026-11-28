package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.ProcessRefundCommand;
import com.team.hotelbooking.payment.application.result.RefundResult;

public interface ProcessRefundUseCase {
  RefundResult execute(ProcessRefundCommand command);
}

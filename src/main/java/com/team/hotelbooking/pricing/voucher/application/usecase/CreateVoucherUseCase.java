package com.team.hotelbooking.pricing.voucher.application.usecase;

import com.team.hotelbooking.pricing.voucher.application.command.CreateVoucherCommand;
import com.team.hotelbooking.pricing.voucher.application.result.VoucherResult;

public interface CreateVoucherUseCase {
  VoucherResult execute(CreateVoucherCommand command);
}

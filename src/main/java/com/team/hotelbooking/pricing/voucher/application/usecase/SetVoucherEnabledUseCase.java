package com.team.hotelbooking.pricing.voucher.application.usecase;

import com.team.hotelbooking.pricing.voucher.application.command.SetVoucherEnabledCommand;
import com.team.hotelbooking.pricing.voucher.application.result.VoucherResult;

public interface SetVoucherEnabledUseCase {
  VoucherResult execute(SetVoucherEnabledCommand command);
}

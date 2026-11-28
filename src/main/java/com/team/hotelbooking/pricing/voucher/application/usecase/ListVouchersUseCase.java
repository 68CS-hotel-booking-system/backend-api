package com.team.hotelbooking.pricing.voucher.application.usecase;

import com.team.hotelbooking.pricing.voucher.application.command.ListVouchersQuery;
import com.team.hotelbooking.pricing.voucher.application.result.VoucherResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListVouchersUseCase {
  PageResult<VoucherResult> execute(ListVouchersQuery query);
}

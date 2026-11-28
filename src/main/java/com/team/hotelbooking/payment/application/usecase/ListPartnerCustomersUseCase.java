package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.ListPartnerCustomersQuery;
import com.team.hotelbooking.payment.application.result.PartnerCustomerResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListPartnerCustomersUseCase {
  PageResult<PartnerCustomerResult> execute(ListPartnerCustomersQuery query);
}

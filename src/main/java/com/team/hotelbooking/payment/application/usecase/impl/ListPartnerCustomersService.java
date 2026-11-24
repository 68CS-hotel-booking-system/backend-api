package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.ListPartnerCustomersQuery;
import com.team.hotelbooking.payment.application.port.PartnerCustomerPort;
import com.team.hotelbooking.payment.application.result.PartnerCustomerResult;
import com.team.hotelbooking.payment.application.usecase.ListPartnerCustomersUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListPartnerCustomersService implements ListPartnerCustomersUseCase {
  private final PartnerCustomerPort customers;
  private final Clock clock;
  private final int maxSize;

  @Override
  public PageResult<PartnerCustomerResult> execute(ListPartnerCustomersQuery query) {
    if (query == null || query.actorId() == null)
      throw FinanceException.accessDenied("Bạn cần đăng nhập.");
    if (query.pagination() == null
        || query.pagination().page() < 0
        || query.pagination().size() < 1
        || query.pagination().size() > maxSize
        || (query.keyword() != null && query.keyword().length() > 100))
      throw new FinanceException(
          com.team.hotelbooking.payment.domain.exception.FinanceErrorCode.VALIDATION_FAILED);
    if (!customers.isActiveAdmin(query.actorId()))
      throw FinanceException.accessDenied(
          "Chỉ quản trị hệ thống được xem danh sách owner đã ký hợp đồng.");
    return customers.findCustomers(
        query.keyword() == null ? "" : query.keyword().trim(), query.pagination(), clock.instant());
  }
}

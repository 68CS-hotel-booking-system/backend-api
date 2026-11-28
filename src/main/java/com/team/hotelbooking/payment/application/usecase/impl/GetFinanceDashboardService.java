package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.GetFinanceDashboardQuery;
import com.team.hotelbooking.payment.application.port.FinanceDashboardPort;
import com.team.hotelbooking.payment.application.result.FinanceDashboardResult;
import com.team.hotelbooking.payment.application.usecase.GetFinanceDashboardUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import java.time.Clock;

public class GetFinanceDashboardService implements GetFinanceDashboardUseCase {
  private final FinanceDashboardPort port;
  private final Clock clock;

  public GetFinanceDashboardService(FinanceDashboardPort port, Clock clock) {
    this.port = port;
    this.clock = clock;
  }

  @Override
  public FinanceDashboardResult execute(GetFinanceDashboardQuery query) {
    if (query == null || query.actorId() == null)
      throw FinanceException.accessDenied("Bạn cần đăng nhập.");
    var viewer =
        port.viewer(query.actorId())
            .orElseThrow(
                () -> FinanceException.accessDenied("Tài khoản không có quyền xem doanh thu."));
    if (!viewer.active()
        || !viewer.emailVerified()
        || (query.admin() ? !viewer.admin() : !viewer.owner()))
      throw FinanceException.accessDenied("Tài khoản không có quyền xem doanh thu.");
    var beneficiaryOwnerId = query.admin() ? null : query.actorId();
    var hotels = port.hotels(query.admin() ? null : query.actorId());
    var hotelIds = hotels.stream().map(FinanceDashboardPort.HotelScope::id).toList();
    var totals = port.summarize(hotelIds, beneficiaryOwnerId);
    var revenues =
        port.contracts(hotelIds, beneficiaryOwnerId).stream()
            .map(FinanceDashboardResult.ContractRevenue::from)
            .toList();
    var now = clock.instant();
    var statuses =
        hotels.stream()
            .map(
                h -> {
                  var state = port.contractState(h.id(), now);
                  return new FinanceDashboardResult.HotelContractStatus(
                      h.id(),
                      h.name(),
                      state.status(),
                      state.endsAt(),
                      h.active() && state.status().equals("EFFECTIVE"));
                })
            .toList();
    return new FinanceDashboardResult(
        totals.bookingCount(),
        totals.bookedAmount(),
        totals.collectedAmount(),
        totals.completedAmount(),
        totals.platformAmount(),
        totals.ownerAmount(),
        totals.missingCommissionCount(),
        totals.pendingRefundAmount(),
        totals.refundedAmount(),
        totals.netCollectedAmount(),
        statuses,
        revenues);
  }
}

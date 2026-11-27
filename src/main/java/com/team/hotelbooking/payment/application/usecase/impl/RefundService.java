package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.*;
import com.team.hotelbooking.payment.application.port.RefundAccessPort;
import com.team.hotelbooking.payment.application.result.RefundResult;
import com.team.hotelbooking.payment.application.usecase.*;
import com.team.hotelbooking.payment.domain.exception.*;
import com.team.hotelbooking.payment.domain.model.*;
import com.team.hotelbooking.payment.domain.repository.RefundRepository;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class RefundService
    implements ListRefundsUseCase, GetRefundUseCase, ProcessRefundUseCase {
  private final RefundRepository refunds;
  private final RefundAccessPort access;
  private final Clock clock;
  private final int maxSize;

  public RefundService(
      RefundRepository refunds, RefundAccessPort access, Clock clock, int maxSize) {
    this.refunds = refunds;
    this.access = access;
    this.clock = clock;
    this.maxSize = maxSize;
  }

  private RefundAccessPort.Viewer viewer(UUID id, boolean management) {
    if (id == null) throw new FinanceException(FinanceErrorCode.ACCESS_DENIED);
    return access
        .viewer(id)
        .filter(v -> v.active() && v.verified() && (!management || v.admin() || v.owner()))
        .orElseThrow(() -> new FinanceException(FinanceErrorCode.ACCESS_DENIED));
  }

  private void requireScope(Refund r, UUID actorId, boolean management, RefundAccessPort.Viewer v) {
    if (management
        ? !v.admin() && !access.hotels(actorId).containsKey(r.hotelId())
        : !actorId.equals(r.customerId()))
      throw new FinanceException(FinanceErrorCode.REFUND_NOT_FOUND);
  }

  public PageResult<RefundResult> execute(ListRefundsQuery q) {
    if (q == null
        || q.pagination() == null
        || q.pagination().page() < 0
        || q.pagination().size() < 1
        || q.pagination().size() > maxSize
        || q.keyword() != null && q.keyword().length() > 100)
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND);
    var v = viewer(q.actorId(), q.management());
    var hotels = access.hotels(q.management() && !v.admin() ? q.actorId() : null);
    Set<UUID> scope = q.management() && !v.admin() ? hotels.keySet() : null;
    return refunds
        .list(
            q.management() ? null : q.actorId(),
            scope,
            q.status(),
            q.bookingId(),
            q.keyword() == null ? "" : q.keyword().trim(),
            q.pagination())
        .map(
            r ->
                RefundResult.from(
                    r,
                    hotels.getOrDefault(r.hotelId(), "Khách sạn"),
                    q.management() && v.admin(),
                    List.of()));
  }

  public RefundResult execute(GetRefundQuery q) {
    if (q == null || q.refundId() == null)
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND);
    var v = viewer(q.actorId(), q.management());
    var r =
        refunds
            .findById(q.refundId())
            .orElseThrow(() -> new FinanceException(FinanceErrorCode.REFUND_NOT_FOUND));
    requireScope(r, q.actorId(), q.management(), v);
    return RefundResult.from(
        r,
        access.hotels(null).getOrDefault(r.hotelId(), "Khách sạn"),
        q.management() && v.admin(),
        refunds.history(r.id()));
  }

  public RefundResult execute(ProcessRefundCommand q) {
    if (q == null || q.refundId() == null || q.expectedVersion() == null || q.expectedVersion() < 0)
      throw new FinanceException(FinanceErrorCode.INVALID_REFUND);
    var v = viewer(q.actorId(), true);
    if (!v.admin()) throw new FinanceException(FinanceErrorCode.ACCESS_DENIED);
    var r =
        refunds
            .findById(q.refundId())
            .orElseThrow(() -> new FinanceException(FinanceErrorCode.REFUND_NOT_FOUND));
    if (r.version() != q.expectedVersion())
      throw new FinanceException(FinanceErrorCode.REFUND_VERSION_CONFLICT);
    var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    var changed = r.process(q.status(), q.reference(), q.note(), now);
    var action =
        new RefundAction(
            UUID.randomUUID(),
            r.id(),
            q.actorId(),
            r.status(),
            changed.status(),
            changed.note(),
            changed.settlementReference(),
            now,
            changed.version());
    if (!refunds.update(changed, r.version(), action))
      throw new FinanceException(FinanceErrorCode.REFUND_VERSION_CONFLICT);
    return RefundResult.from(
        changed,
        access.hotels(null).getOrDefault(r.hotelId(), "Khách sạn"),
        true,
        refunds.history(r.id()));
  }
}

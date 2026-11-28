package com.team.hotelbooking.pricing.voucher.application.usecase.impl;

import com.team.hotelbooking.pricing.voucher.application.command.*;
import com.team.hotelbooking.pricing.voucher.application.port.VoucherAccessPort;
import com.team.hotelbooking.pricing.voucher.application.result.VoucherResult;
import com.team.hotelbooking.pricing.voucher.application.usecase.*;
import com.team.hotelbooking.pricing.voucher.domain.exception.*;
import com.team.hotelbooking.pricing.voucher.domain.model.Voucher;
import com.team.hotelbooking.pricing.voucher.domain.repository.VoucherRepository;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.*;
import java.util.*;

public final class VoucherService
    implements com.team.hotelbooking.pricing.voucher.application.port.VoucherDiscountPort {
  private final VoucherRepository vouchers;
  private final VoucherAccessPort access;
  private final Clock clock;
  private final int maxSize;

  public VoucherService(
      VoucherRepository vouchers, VoucherAccessPort access, Clock clock, int maxSize) {
    this.vouchers = vouchers;
    this.access = access;
    this.clock = clock;
    this.maxSize = maxSize;
  }

  private void owner(UUID actor, UUID hotel) {
    if (actor == null || hotel == null || !access.isCurrentOwner(actor, hotel))
      throw new VoucherException(VoucherErrorCode.VOUCHER_ACCESS_DENIED);
  }

  public VoucherResult execute(CreateVoucherCommand c) {
    if (c == null) throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    owner(c.actorId(), c.hotelId());
    var now = clock.instant();
    var v =
        Voucher.create(
            c.hotelId(),
            c.actorId(),
            c.code(),
            c.name(),
            c.discountType(),
            c.discountValue(),
            c.maxDiscountAmount(),
            c.minOrderAmount(),
            c.startsAt(),
            c.endsAt(),
            c.usageLimit(),
            now);
    if (vouchers.find(v.hotelId(), v.code(), false).isPresent())
      throw new VoucherException(VoucherErrorCode.VOUCHER_DUPLICATE);
    return VoucherResult.from(vouchers.add(v), 0, now);
  }

  public PageResult<VoucherResult> execute(ListVouchersQuery q) {
    if (q == null
        || q.page() == null
        || q.page().page() < 0
        || q.page().size() < 1
        || q.page().size() > maxSize) throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    owner(q.actorId(), q.hotelId());
    var now = clock.instant();
    return vouchers
        .list(q.hotelId(), q.page())
        .map(v -> VoucherResult.from(v, vouchers.occupied(v.id(), now), now));
  }

  public VoucherResult execute(SetVoucherEnabledCommand c) {
    if (c == null || c.voucherId() == null || c.expectedVersion() < 0)
      throw new VoucherException(VoucherErrorCode.VOUCHER_INVALID);
    owner(c.actorId(), c.hotelId());
    var v =
        vouchers
            .findById(c.voucherId(), true)
            .filter(x -> x.hotelId().equals(c.hotelId()))
            .orElseThrow(() -> new VoucherException(VoucherErrorCode.VOUCHER_NOT_FOUND));
    if (v.version() != c.expectedVersion()
        || !vouchers.setEnabled(v.id(), c.expectedVersion(), c.enabled()))
      throw new VoucherException(VoucherErrorCode.VOUCHER_VERSION_CONFLICT);
    var updated = vouchers.findById(v.id(), false).orElseThrow();
    return VoucherResult.from(updated, vouchers.occupied(v.id(), clock.instant()), clock.instant());
  }

  public long discount(UUID hotel, String code, long roomAmount) {
    String normalized = Voucher.normalize(code);
    if (normalized == null) return 0;
    var v =
        vouchers
            .find(hotel, normalized, false)
            .orElseThrow(() -> new VoucherException(VoucherErrorCode.VOUCHER_INVALID));
    var now = clock.instant();
    return v.discount(roomAmount, now, vouchers.occupied(v.id(), now));
  }

  public void reserve(
      UUID hotel, String code, UUID booking, long roomAmount, long discount, Instant expiresAt) {
    String normalized = Voucher.normalize(code);
    if (normalized == null) return;
    var v =
        vouchers
            .find(hotel, normalized, true)
            .orElseThrow(() -> new VoucherException(VoucherErrorCode.VOUCHER_INVALID));
    var now = clock.instant();
    if (expiresAt == null || !expiresAt.isAfter(now))
      throw new VoucherException(VoucherErrorCode.VOUCHER_UNAVAILABLE);
    if (v.discount(roomAmount, now, vouchers.occupied(v.id(), now)) != discount)
      throw new VoucherException(VoucherErrorCode.VOUCHER_PRICE_CHANGED);
    vouchers.reserve(v, booking, roomAmount, discount, expiresAt, now);
  }

  public void lockForPayment(UUID booking) {
    vouchers
        .reservation(booking)
        .ifPresent(r -> vouchers.findById(r.voucherId(), true).orElseThrow());
  }

  public void consume(UUID booking) {
    vouchers.consume(booking);
  }

  public void release(UUID booking) {
    vouchers.release(booking);
  }

  public String bookingCode(UUID booking) {
    return vouchers.reservation(booking).map(VoucherRepository.Reservation::code).orElse(null);
  }
}

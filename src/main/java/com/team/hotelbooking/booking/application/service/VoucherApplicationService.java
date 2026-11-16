package com.team.hotelbooking.booking.application.service;

import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Voucher;
import com.team.hotelbooking.booking.domain.repository.VoucherRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class VoucherApplicationService {

  private final VoucherRepository vouchers;
  private final Clock clock;

  public VoucherApplicationService(VoucherRepository vouchers, Clock clock) {
    this.vouchers = vouchers;
    this.clock = clock;
  }

  public Application evaluate(String code, UUID hotelId, UUID userId, long subtotal, boolean lock) {
    Voucher voucher =
        vouchers
            .findByCode(Voucher.normalizeCode(code), lock)
            .orElseThrow(() -> new BookingException(BookingErrorCode.VOUCHER_INVALID));
    Instant now = clock.instant();
    long discount = voucher.discountFor(hotelId, subtotal, now);
    VoucherRepository.Usage usage = vouchers.countUsage(voucher.id(), userId, now);
    if ((voucher.usageLimit() != null && usage.total() >= voucher.usageLimit())
        || usage.byUser() >= voucher.perUserLimit()) {
      throw new BookingException(BookingErrorCode.VOUCHER_LIMIT_REACHED);
    }
    return new Application(voucher, discount, now);
  }

  public record Application(Voucher voucher, long discount, Instant evaluatedAt) {}
}

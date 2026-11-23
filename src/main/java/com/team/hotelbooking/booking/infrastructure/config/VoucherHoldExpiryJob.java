package com.team.hotelbooking.booking.infrastructure.config;

import com.team.hotelbooking.booking.domain.repository.VoucherRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public final class VoucherHoldExpiryJob {

  private static final Logger log = LoggerFactory.getLogger(VoucherHoldExpiryJob.class);
  private static final int BATCH_SIZE = 100;

  private final VoucherRepository vouchers;
  private final Consumer<UUID> expire;
  private final Clock clock;

  public VoucherHoldExpiryJob(VoucherRepository vouchers, Consumer<UUID> expire, Clock clock) {
    this.vouchers = vouchers;
    this.expire = expire;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${booking.voucher-expiry-delay:PT1M}")
  public void expireDueBookings() {
    for (UUID bookingId : vouchers.findExpiredBookingIds(clock.instant(), BATCH_SIZE)) {
      try {
        expire.accept(bookingId);
      } catch (RuntimeException exception) {
        log.warn("Could not expire voucher booking {}", bookingId, exception);
      }
    }
  }
}

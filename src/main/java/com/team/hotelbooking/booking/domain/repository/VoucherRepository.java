package com.team.hotelbooking.booking.domain.repository;

import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Voucher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository {
  Optional<Voucher> findByCode(String code, boolean lock);

  Usage countUsage(UUID voucherId, UUID userId, Instant now);

  void record(Booking booking, Voucher voucher);

  void release(UUID bookingId, Instant now);

  List<UUID> findExpiredBookingIds(Instant now, int limit);

  void save(Voucher voucher, Instant now, boolean create);

  record Usage(long total, long byUser) {}
}

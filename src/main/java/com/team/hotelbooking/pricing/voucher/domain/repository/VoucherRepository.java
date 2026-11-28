package com.team.hotelbooking.pricing.voucher.domain.repository;

import com.team.hotelbooking.pricing.voucher.domain.model.Voucher;
import com.team.hotelbooking.shared.pagination.*;
import java.time.Instant;
import java.util.*;

public interface VoucherRepository {
  Optional<Voucher> find(UUID hotelId, String code, boolean lock);

  Optional<Voucher> findById(UUID id, boolean lock);

  Voucher add(Voucher value);

  boolean setEnabled(UUID id, long version, boolean enabled);

  PageResult<Voucher> list(UUID hotelId, PageQuery page);

  long occupied(UUID id, Instant now);

  void reserve(
      Voucher voucher,
      UUID bookingId,
      long roomAmount,
      long discount,
      Instant expiresAt,
      Instant now);

  Optional<Reservation> reservation(UUID bookingId);

  void consume(UUID bookingId);

  void release(UUID bookingId);

  record Reservation(UUID voucherId, String code) {}
}

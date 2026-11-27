package com.team.hotelbooking.pricing.voucher.infrastructure.persistence.adapter;

import com.team.hotelbooking.pricing.voucher.domain.exception.*;
import com.team.hotelbooking.pricing.voucher.domain.model.Voucher;
import com.team.hotelbooking.pricing.voucher.domain.repository.VoucherRepository;
import com.team.hotelbooking.pricing.voucher.infrastructure.persistence.entity.VoucherRedemptionJpaEntity;
import com.team.hotelbooking.pricing.voucher.infrastructure.persistence.mapper.VoucherPersistenceMapper;
import com.team.hotelbooking.pricing.voucher.infrastructure.persistence.repository.*;
import com.team.hotelbooking.shared.pagination.*;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaVoucherRepositoryAdapter implements VoucherRepository {
  private final VoucherJpaRepository vouchers;
  private final VoucherRedemptionJpaRepository redemptions;

  public Optional<Voucher> find(UUID hotel, String code, boolean lock) {
    return (lock ? vouchers.lockCode(hotel, code) : vouchers.findByHotelIdAndCode(hotel, code))
        .map(VoucherPersistenceMapper::toDomain);
  }

  public Optional<Voucher> findById(UUID id, boolean lock) {
    return (lock ? vouchers.lockId(id) : vouchers.findById(id))
        .map(VoucherPersistenceMapper::toDomain);
  }

  public Voucher add(Voucher value) {
    try {
      return VoucherPersistenceMapper.toDomain(
          vouchers.saveAndFlush(VoucherPersistenceMapper.toEntity(value)));
    } catch (DataIntegrityViolationException e) {
      if (e.getMostSpecificCause().getMessage().contains("uq_hotel_voucher_code"))
        throw new VoucherException(VoucherErrorCode.VOUCHER_DUPLICATE);
      throw e;
    }
  }

  public boolean setEnabled(UUID id, long version, boolean enabled) {
    return vouchers.setEnabled(id, version, enabled) == 1;
  }

  public PageResult<Voucher> list(UUID hotel, PageQuery p) {
    return PageResultMapper.from(
        vouchers.findByHotelId(
            hotel,
            PageRequest.of(
                p.page(), p.size(), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))),
        VoucherPersistenceMapper::toDomain);
  }

  public long occupied(UUID id, Instant now) {
    return redemptions.occupied(id, now);
  }

  public void reserve(
      Voucher v, UUID booking, long room, long discount, Instant expires, Instant now) {
    var r = new VoucherRedemptionJpaEntity();
    r.setBookingId(booking);
    r.setVoucherId(v.id());
    r.setCode(v.code());
    r.setDiscountType(v.discountType());
    r.setDiscountValue(v.discountValue());
    r.setMaxDiscountAmount(v.maxDiscountAmount());
    r.setMinOrderAmount(v.minOrderAmount());
    r.setRoomAmount(room);
    r.setDiscountAmount(discount);
    r.setExpiresAt(expires);
    r.setCreatedAt(now);
    r.setStatus("RESERVED");
    redemptions.saveAndFlush(r);
  }

  public Optional<Reservation> reservation(UUID booking) {
    return redemptions.findById(booking).map(r -> new Reservation(r.getVoucherId(), r.getCode()));
  }

  public void consume(UUID booking) {
    redemptions.consume(booking);
  }

  public void release(UUID booking) {
    redemptions.release(booking);
  }
}

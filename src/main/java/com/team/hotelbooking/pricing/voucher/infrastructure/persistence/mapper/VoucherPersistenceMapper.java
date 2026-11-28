package com.team.hotelbooking.pricing.voucher.infrastructure.persistence.mapper;

import com.team.hotelbooking.pricing.voucher.domain.model.Voucher;
import com.team.hotelbooking.pricing.voucher.infrastructure.persistence.entity.VoucherJpaEntity;

public final class VoucherPersistenceMapper {
  private VoucherPersistenceMapper() {}

  public static Voucher toDomain(VoucherJpaEntity v) {
    return new Voucher(
        v.getId(),
        v.getHotelId(),
        v.getCode(),
        v.getName(),
        v.getDiscountType(),
        v.getDiscountValue(),
        v.getMaxDiscountAmount(),
        v.getMinOrderAmount(),
        v.getStartsAt(),
        v.getEndsAt(),
        v.getUsageLimit(),
        v.isEnabled(),
        v.getCreatedBy(),
        v.getCreatedAt(),
        v.getVersion());
  }

  public static VoucherJpaEntity toEntity(Voucher v) {
    var e = new VoucherJpaEntity();
    e.setId(v.id());
    e.setHotelId(v.hotelId());
    e.setCode(v.code());
    e.setName(v.name());
    e.setDiscountType(v.discountType());
    e.setDiscountValue(v.discountValue());
    e.setMaxDiscountAmount(v.maxDiscountAmount());
    e.setMinOrderAmount(v.minOrderAmount());
    e.setStartsAt(v.startsAt());
    e.setEndsAt(v.endsAt());
    e.setUsageLimit(v.usageLimit());
    e.setEnabled(v.enabled());
    e.setCreatedBy(v.createdBy());
    e.setCreatedAt(v.createdAt());
    return e;
  }
}

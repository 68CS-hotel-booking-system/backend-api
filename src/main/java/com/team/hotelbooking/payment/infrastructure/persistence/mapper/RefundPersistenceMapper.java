package com.team.hotelbooking.payment.infrastructure.persistence.mapper;

import com.team.hotelbooking.payment.domain.model.Refund;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.RefundJpaEntity;

public final class RefundPersistenceMapper {
  private RefundPersistenceMapper() {}

  public static Refund toDomain(RefundJpaEntity e) {
    return new Refund(
        e.getId(),
        e.getPaymentId(),
        e.getBookingId(),
        e.getCustomerId(),
        e.getHotelId(),
        e.getBookingCode(),
        e.getBookerName(),
        e.getBookerEmail(),
        e.getBookerPhone(),
        e.getAmount(),
        e.getCurrency(),
        e.getReason(),
        e.getCustomerNote(),
        e.getStatus(),
        e.getSettlementReference(),
        e.getNote(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getCompletedAt(),
        e.getVersion());
  }

  public static RefundJpaEntity toEntity(Refund r) {
    var e = new RefundJpaEntity();
    e.setId(r.id());
    e.setPaymentId(r.paymentId());
    e.setBookingId(r.bookingId());
    e.setCustomerId(r.customerId());
    e.setHotelId(r.hotelId());
    e.setBookingCode(r.bookingCode());
    e.setBookerName(r.bookerName());
    e.setBookerEmail(r.bookerEmail());
    e.setBookerPhone(r.bookerPhone());
    e.setAmount(r.amount());
    e.setCurrency(r.currency());
    e.setReason(r.reason());
    e.setCustomerNote(r.customerNote());
    e.setStatus(r.status());
    e.setSettlementReference(r.settlementReference());
    e.setNote(r.note());
    e.setCreatedAt(r.createdAt());
    e.setUpdatedAt(r.updatedAt());
    e.setCompletedAt(r.completedAt());
    e.setVersion(r.version());
    return e;
  }
}

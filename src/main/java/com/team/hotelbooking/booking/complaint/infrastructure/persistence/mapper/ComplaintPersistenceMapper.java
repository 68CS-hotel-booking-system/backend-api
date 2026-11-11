package com.team.hotelbooking.booking.complaint.infrastructure.persistence.mapper;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.booking.complaint.infrastructure.persistence.entity.ComplaintJpaEntity;

public final class ComplaintPersistenceMapper {

  private ComplaintPersistenceMapper() {}

  public static Complaint toDomain(ComplaintJpaEntity entity) {
    return new Complaint(
        entity.getId(),
        entity.getBookingId(),
        entity.getCustomerId(),
        entity.getHotelId(),
        Complaint.Category.valueOf(entity.getCategory().name()),
        entity.getContent(),
        Complaint.Status.valueOf(entity.getStatus().name()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion() == null ? 0L : entity.getVersion());
  }

  public static ComplaintJpaEntity.DbStatus toDbStatus(Complaint.Status status) {
    return ComplaintJpaEntity.DbStatus.valueOf(status.name());
  }
}

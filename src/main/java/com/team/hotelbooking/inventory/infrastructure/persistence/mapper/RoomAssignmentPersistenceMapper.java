package com.team.hotelbooking.inventory.infrastructure.persistence.mapper;

import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomAssignmentPersistenceMapper {

  public RoomAssignment toDomain(RoomAssignmentJpaEntity entity) {
    return new RoomAssignment(
        entity.getId(),
        entity.getHotelId(),
        entity.getBookingId(),
        entity.getBookingItemId(),
        entity.getRoomTypeId(),
        entity.getRoomId(),
        entity.getRoomNumberSnapshot(),
        entity.getStartDate(),
        entity.getEndDate(),
        RoomAssignment.Status.valueOf(entity.getStatus().name()),
        entity.getAssignedByUserId(),
        entity.getAssignedAt(),
        entity.getCheckedInByUserId(),
        entity.getCheckedInAt(),
        entity.getCheckedOutByUserId(),
        entity.getCheckedOutAt(),
        entity.getReleasedByUserId(),
        entity.getReleasedAt(),
        entity.getReleaseReason());
  }

  public RoomAssignmentJpaEntity toEntity(RoomAssignment domain) {
    RoomAssignmentJpaEntity entity = new RoomAssignmentJpaEntity();
    entity.setId(domain.id());
    entity.setHotelId(domain.hotelId());
    entity.setBookingId(domain.bookingId());
    entity.setBookingItemId(domain.bookingItemId());
    entity.setRoomTypeId(domain.roomTypeId());
    entity.setRoomId(domain.roomId());
    entity.setRoomNumberSnapshot(domain.roomNumberSnapshot());
    entity.setStartDate(domain.startDate());
    entity.setEndDate(domain.endDate());
    entity.setStatus(
        RoomAssignmentJpaEntity.DbRoomAssignmentStatus.valueOf(domain.status().name()));
    entity.setAssignedByUserId(domain.assignedByUserId());
    entity.setAssignedAt(domain.assignedAt());
    entity.setCheckedInByUserId(domain.checkedInByUserId());
    entity.setCheckedInAt(domain.checkedInAt());
    entity.setCheckedOutByUserId(domain.checkedOutByUserId());
    entity.setCheckedOutAt(domain.checkedOutAt());
    entity.setReleasedByUserId(domain.releasedByUserId());
    entity.setReleasedAt(domain.releasedAt());
    entity.setReleaseReason(domain.releaseReason());
    return entity;
  }
}

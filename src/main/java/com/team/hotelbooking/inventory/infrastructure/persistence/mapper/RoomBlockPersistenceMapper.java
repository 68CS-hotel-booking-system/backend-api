package com.team.hotelbooking.inventory.infrastructure.persistence.mapper;

import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity.DbRoomBlockStatus;

public final class RoomBlockPersistenceMapper {

  private RoomBlockPersistenceMapper() {}

  public static RoomBlock toDomain(RoomBlockJpaEntity entity) {
    return new RoomBlock(
        entity.getId(),
        entity.getHotelId(),
        entity.getRoomTypeId(),
        entity.getRoomId(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getReason(),
        entity.getStatus() == DbRoomBlockStatus.RELEASED
            ? RoomBlock.Status.RELEASED
            : RoomBlock.Status.ACTIVE,
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedByUserId(),
        entity.getUpdatedAt(),
        entity.getReleasedByUserId(),
        entity.getReleasedAt(),
        entity.getReleaseReason());
  }

  public static RoomBlockJpaEntity toEntity(RoomBlock domain) {
    RoomBlockJpaEntity entity = new RoomBlockJpaEntity();
    entity.setId(domain.id());
    entity.setHotelId(domain.hotelId());
    entity.setRoomTypeId(domain.roomTypeId());
    entity.setRoomId(domain.roomId());
    entity.setStartDate(domain.startDate());
    entity.setEndDate(domain.endDate());
    entity.setReason(domain.reason());
    entity.setStatus(
        domain.status() == RoomBlock.Status.RELEASED
            ? DbRoomBlockStatus.RELEASED
            : DbRoomBlockStatus.ACTIVE);
    entity.setCreatedByUserId(domain.createdByUserId());
    entity.setCreatedAt(domain.createdAt());
    entity.setUpdatedByUserId(domain.updatedByUserId());
    entity.setUpdatedAt(domain.updatedAt());
    entity.setReleasedByUserId(domain.releasedByUserId());
    entity.setReleasedAt(domain.releasedAt());
    entity.setReleaseReason(domain.releaseReason());
    return entity;
  }
}

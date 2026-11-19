package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;

public final class RoomPersistenceMapper {

  private RoomPersistenceMapper() {}

  public static RoomJpaEntity toNewEntity(Room room) {
    RoomJpaEntity entity = new RoomJpaEntity();
    entity.setId(room.id());
    entity.setHotelId(room.hotelId());
    entity.setRoomTypeId(room.roomTypeId());
    entity.setRoomNumber(room.roomNumber());
    entity.setFloor(room.floor());
    entity.setReadiness(RoomJpaEntity.DbReadiness.valueOf(room.readiness().name()));
    entity.setReadinessUpdatedAt(room.readinessUpdatedAt());
    entity.setReadinessUpdatedBy(room.readinessUpdatedBy());
    entity.setOperationalFrom(room.operationalFrom());
    entity.setOperationalUntil(room.operationalUntil());
    entity.setCreatedAt(room.createdAt());
    entity.setUpdatedAt(room.updatedAt());

    // Để version null cho entity mới; Hibernate quản lý @Version.
    return entity;
  }

  // Chỉ chép field HT21 được sửa; giữ mốc readiness, operationalFrom, createdAt, version.
  public static void applyUpdate(RoomJpaEntity entity, Room room) {
    entity.setRoomNumber(room.roomNumber());
    entity.setFloor(room.floor());
    entity.setReadiness(RoomJpaEntity.DbReadiness.valueOf(room.readiness().name()));
    entity.setOperationalUntil(room.operationalUntil());
    entity.setUpdatedAt(room.updatedAt());
  }

  public static Room toDomain(RoomJpaEntity entity) {
    return new Room(
        entity.getId(),
        entity.getHotelId(),
        entity.getRoomTypeId(),
        entity.getRoomNumber(),
        entity.getFloor(),
        Room.Readiness.valueOf(entity.getReadiness().name()),
        entity.getReadinessUpdatedAt(),
        entity.getReadinessUpdatedBy(),
        entity.getOperationalFrom(),
        entity.getOperationalUntil(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }
}

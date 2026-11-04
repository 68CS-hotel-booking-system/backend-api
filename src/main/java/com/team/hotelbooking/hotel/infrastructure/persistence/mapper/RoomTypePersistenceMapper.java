package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;

public final class RoomTypePersistenceMapper {

  private RoomTypePersistenceMapper() {}

  public static RoomTypeJpaEntity toNewEntity(RoomType roomType) {
    RoomTypeJpaEntity entity = new RoomTypeJpaEntity();
    entity.setId(roomType.id());
    entity.setHotelId(roomType.hotelId());
    entity.setName(roomType.name());
    entity.setDescription(roomType.description());
    entity.setMaxAdults(roomType.maxAdults());
    entity.setMaxChildren(roomType.maxChildren());
    entity.setActive(roomType.active());
    entity.setCreatedAt(roomType.createdAt());
    entity.setUpdatedAt(roomType.updatedAt());

    // Để version null cho entity mới; Hibernate quản lý @Version.
    return entity;
  }

  public static RoomType toDomain(RoomTypeJpaEntity entity) {
    return new RoomType(
        entity.getId(),
        entity.getHotelId(),
        entity.getName(),
        entity.getDescription(),
        entity.getMaxAdults(),
        entity.getMaxChildren(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }
}

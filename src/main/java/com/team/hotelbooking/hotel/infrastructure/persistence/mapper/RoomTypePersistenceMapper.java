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
    entity.setMaxOccupancy(roomType.maxOccupancy());
    entity.setBasePrice(roomType.basePrice());
    entity.setActive(roomType.active());
    entity.setCreatedAt(roomType.createdAt());
    entity.setUpdatedAt(roomType.updatedAt());

    // Để version null cho entity mới; Hibernate quản lý @Version.
    return entity;
  }

  // Chỉ chép field HT17 được sửa; id, hotelId, createdAt, version giữ theo entity hiện có.
  public static void applyUpdate(RoomTypeJpaEntity entity, RoomType roomType) {
    entity.setName(roomType.name());
    entity.setDescription(roomType.description());
    entity.setMaxAdults(roomType.maxAdults());
    entity.setMaxChildren(roomType.maxChildren());
    entity.setMaxOccupancy(roomType.maxOccupancy());
    entity.setBasePrice(roomType.basePrice());
    entity.setActive(roomType.active());
    entity.setUpdatedAt(roomType.updatedAt());
  }

  public static RoomType toDomain(RoomTypeJpaEntity entity) {
    return new RoomType(
        entity.getId(),
        entity.getHotelId(),
        entity.getName(),
        entity.getDescription(),
        entity.getMaxAdults(),
        entity.getMaxChildren(),
        entity.getMaxOccupancy(),
        entity.getBasePrice(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }
}

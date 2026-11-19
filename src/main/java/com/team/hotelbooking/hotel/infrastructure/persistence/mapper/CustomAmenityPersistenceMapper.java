package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.CustomAmenity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.CustomAmenityJpaEntity;

public final class CustomAmenityPersistenceMapper {
  private CustomAmenityPersistenceMapper() {}

  public static CustomAmenity toDomain(CustomAmenityJpaEntity item) {
    return new CustomAmenity(
        item.getId(),
        item.getHotelId(),
        item.getRoomId(),
        item.getName(),
        item.getNormalizedName(),
        item.getDescription(),
        item.isAttached(),
        item.getCreatedAt(),
        item.getUpdatedAt(),
        item.getVersion());
  }

  public static CustomAmenityJpaEntity toNewEntity(CustomAmenity item) {
    CustomAmenityJpaEntity entity = new CustomAmenityJpaEntity();
    entity.setId(item.id());
    entity.setHotelId(item.hotelId());
    entity.setRoomId(item.roomId());
    entity.setCreatedAt(item.createdAt());
    applyChanges(entity, item);
    return entity;
  }

  public static void applyChanges(CustomAmenityJpaEntity entity, CustomAmenity item) {
    entity.setName(item.name());
    entity.setNormalizedName(item.normalizedName());
    entity.setDescription(item.description());
    entity.setAttached(item.attached());
    entity.setUpdatedAt(item.updatedAt());
  }
}

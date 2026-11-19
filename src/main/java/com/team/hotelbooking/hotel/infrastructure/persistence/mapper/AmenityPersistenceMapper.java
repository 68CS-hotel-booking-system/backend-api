package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.AmenityJpaEntity;

public final class AmenityPersistenceMapper {

  private AmenityPersistenceMapper() {}

  public static Amenity toDomain(AmenityJpaEntity entity) {
    return new Amenity(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        Amenity.Category.valueOf(entity.getCategory().name()));
  }

  public static AmenityJpaEntity.DbCategory toDbCategory(Amenity.Category category) {
    return AmenityJpaEntity.DbCategory.valueOf(category.name());
  }
}

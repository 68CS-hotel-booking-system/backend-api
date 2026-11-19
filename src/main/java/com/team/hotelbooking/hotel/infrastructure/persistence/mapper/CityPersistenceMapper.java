package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.City;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.CityJpaEntity;

public final class CityPersistenceMapper {

  private CityPersistenceMapper() {}

  public static City toDomain(CityJpaEntity entity) {
    return new City(entity.getId(), entity.getCountryCode(), entity.getName(), entity.getSlug());
  }
}

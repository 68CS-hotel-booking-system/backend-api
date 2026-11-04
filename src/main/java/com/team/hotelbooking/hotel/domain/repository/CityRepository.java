package com.team.hotelbooking.hotel.domain.repository;

import java.util.UUID;

public interface CityRepository {
  boolean existsById(UUID cityId);
}

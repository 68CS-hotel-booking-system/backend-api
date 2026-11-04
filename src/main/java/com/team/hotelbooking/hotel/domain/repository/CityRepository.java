package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.City;
import java.util.List;
import java.util.UUID;

public interface CityRepository {
  boolean existsById(UUID cityId);

  List<City> findAll();
}

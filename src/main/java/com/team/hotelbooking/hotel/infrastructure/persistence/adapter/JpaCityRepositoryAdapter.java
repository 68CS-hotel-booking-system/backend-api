package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.model.City;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.CityPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.CityJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaCityRepositoryAdapter implements CityRepository {

  private final CityJpaRepository cities;

  @Override
  public boolean existsById(UUID cityId) {
    return cities.existsById(cityId);
  }

  @Override
  public List<City> findAll() {
    return cities.findAllByOrderByCountryCodeAscNameAscIdAsc().stream()
        .map(CityPersistenceMapper::toDomain)
        .toList();
  }
}

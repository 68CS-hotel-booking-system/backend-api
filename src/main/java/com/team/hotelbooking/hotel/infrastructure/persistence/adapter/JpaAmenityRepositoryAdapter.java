package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.AmenityPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.AmenityJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaAmenityRepositoryAdapter implements AmenityRepository {

  private final AmenityJpaRepository amenities;

  @Override
  public List<Amenity> findAll() {
    return amenities.findAllByOrderByCategoryAscNameAscIdAsc().stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Amenity> findByCategory(Amenity.Category category) {
    return amenities
        .findAllByCategoryOrderByNameAscIdAsc(AmenityPersistenceMapper.toDbCategory(category))
        .stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }
}

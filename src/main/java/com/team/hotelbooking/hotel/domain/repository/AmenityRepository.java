package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import java.util.List;

public interface AmenityRepository {

  List<Amenity> findAll();

  List<Amenity> findByCategory(Amenity.Category category);
}

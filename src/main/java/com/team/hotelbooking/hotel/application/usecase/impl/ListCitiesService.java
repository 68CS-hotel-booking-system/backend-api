package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.ListCitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ListCitiesUseCase;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import java.util.List;

public class ListCitiesService implements ListCitiesUseCase {

  private final CityRepository cities;

  public ListCitiesService(CityRepository cities) {
    this.cities = cities;
  }

  @Override
  public List<ListCitiesResult> execute() {
    // Thứ tự countryCode, name, id do repository bảo đảm; service giữ nguyên thứ tự đó.
    return cities.findAll().stream().map(ListCitiesResult::from).toList();
  }
}

package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.hotel.application.result.ListCitiesResult;
import com.team.hotelbooking.hotel.domain.model.City;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Danh mục thành phố")
class ListCitiesServiceTests {

  private static final City HA_NOI =
      new City(UUID.fromString("81560b64-8216-46c6-8f1b-f14214fc0c90"), "VN", "Hà Nội", "ha-noi");
  private static final City HUE =
      new City(UUID.fromString("d4266df8-21c1-4f42-849a-30bebfc561b9"), "VN", "Huế", "hue");

  @Mock private CityRepository cities;

  private ListCitiesService service;

  @BeforeEach
  void setUp() {
    service = new ListCitiesService(cities);
  }

  @Nested
  @DisplayName("ListCitiesUseCase.execute")
  class ListCitiesTests {

    @Test
    void shouldMapCitiesInRepositoryOrderWhenCitiesExist() {
      // Given
      given(cities.findAll()).willReturn(List.of(HUE, HA_NOI));

      // When
      List<ListCitiesResult> result = service.execute();

      // Then
      assertEquals(
          List.of(
              new ListCitiesResult(HUE.id(), "VN", "Huế", "hue"),
              new ListCitiesResult(HA_NOI.id(), "VN", "Hà Nội", "ha-noi")),
          result);
    }

    @Test
    void shouldReturnEmptyListWhenNoCityExists() {
      // Given
      given(cities.findAll()).willReturn(List.of());

      // When
      List<ListCitiesResult> result = service.execute();

      // Then
      assertTrue(result.isEmpty());
    }
  }
}

package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Danh mục tiện ích")
class ListAmenitiesServiceTests {

  private static final Amenity SWIMMING_POOL =
      new Amenity(
          UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3"),
          "SWIMMING_POOL",
          "Hồ bơi",
          Amenity.Category.HOTEL);
  private static final Amenity FREE_WIFI =
      new Amenity(
          UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a"),
          "FREE_WIFI",
          "Wi-Fi miễn phí",
          Amenity.Category.ROOM);

  @Mock private AmenityRepository amenities;

  private ListAmenitiesService service;

  @BeforeEach
  void setUp() {
    service = new ListAmenitiesService(amenities);
  }

  @Nested
  @DisplayName("ListAmenitiesUseCase.execute")
  class ListAmenitiesTests {

    @Test
    void shouldReturnAllAmenitiesInRepositoryOrderWhenCategoryIsMissing() {
      // Given
      given(amenities.findAll()).willReturn(List.of(SWIMMING_POOL, FREE_WIFI));

      // When
      List<ListAmenitiesResult> result = service.execute(null);

      // Then
      assertEquals(
          List.of(
              new ListAmenitiesResult(SWIMMING_POOL.id(), "SWIMMING_POOL", "Hồ bơi", "HOTEL"),
              new ListAmenitiesResult(FREE_WIFI.id(), "FREE_WIFI", "Wi-Fi miễn phí", "ROOM")),
          result);
    }

    @Test
    void shouldFilterByCategoryWhenCategoryIsHotel() {
      // Given
      given(amenities.findByCategory(Amenity.Category.HOTEL)).willReturn(List.of(SWIMMING_POOL));

      // When
      List<ListAmenitiesResult> result = service.execute("HOTEL");

      // Then
      assertEquals(
          List.of(new ListAmenitiesResult(SWIMMING_POOL.id(), "SWIMMING_POOL", "Hồ bơi", "HOTEL")),
          result);
    }

    @Test
    void shouldReturnEmptyListWhenCategoryHasNoAmenity() {
      // Given
      given(amenities.findByCategory(Amenity.Category.ROOM)).willReturn(List.of());

      // When
      List<ListAmenitiesResult> result = service.execute("ROOM");

      // Then
      assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hotel", "Room", "SPA", "", " HOTEL"})
    void shouldRejectRequestWhenCategoryIsNotExactlyHotelOrRoom(String category) {
      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(category));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals("Loại tiện ích phải là HOTEL hoặc ROOM.", error.getMessage());
      then(amenities).shouldHaveNoInteractions();
    }
  }
}

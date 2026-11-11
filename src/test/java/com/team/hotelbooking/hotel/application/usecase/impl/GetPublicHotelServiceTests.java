package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Chi tiết khách sạn công khai")
class GetPublicHotelServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID OWNER_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID CITY_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");

  private static final Amenity POOL =
      new Amenity(
          UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3"),
          "SWIMMING_POOL",
          "Hồ bơi",
          Amenity.Category.HOTEL);

  @Mock private HotelRepository hotels;

  @Mock private AmenityRepository amenities;

  private GetPublicHotelService service;
  private Hotel activeHotel;

  @BeforeEach
  void setUp() {
    service = new GetPublicHotelService(hotels, amenities);
    activeHotel =
        new Hotel(
            HOTEL_ID,
            OWNER_ID,
            CITY_ID,
            "Khách sạn Biển Xanh",
            "khach-san-bien-xanh-" + HOTEL_ID,
            "Gần biển Mỹ Khê",
            "12 Võ Nguyên Giáp",
            null,
            null,
            new BigDecimal("16.061400"),
            new BigDecimal("108.247800"),
            (short) 4,
            LocalTime.of(14, 0),
            LocalTime.of(12, 0),
            Hotel.Status.ACTIVE,
            NOW,
            NOW,
            3L);
  }

  @Nested
  @DisplayName("GetPublicHotelUseCase.execute")
  class GetPublicHotelTests {

    @Test
    void shouldReturnPublicFieldsWhenHotelIsActive() {
      // Given
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE))
          .willReturn(Optional.of(activeHotel));
      given(amenities.findByHotelId(HOTEL_ID)).willReturn(List.of(POOL));

      // When
      GetPublicHotelResult result = service.execute(HOTEL_ID);

      // Then
      assertEquals(
          new GetPublicHotelResult(
              HOTEL_ID,
              CITY_ID,
              "Khách sạn Biển Xanh",
              "khach-san-bien-xanh-" + HOTEL_ID,
              "Gần biển Mỹ Khê",
              "12 Võ Nguyên Giáp",
              new BigDecimal("16.061400"),
              new BigDecimal("108.247800"),
              (short) 4,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              List.of(new ListAmenitiesResult(POOL.id(), "SWIMMING_POOL", "Hồ bơi", "HOTEL"))),
          result);
    }

    @Test
    void shouldReturnEmptyAmenitiesWhenHotelHasNoAmenity() {
      // Given
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE))
          .willReturn(Optional.of(activeHotel));
      given(amenities.findByHotelId(HOTEL_ID)).willReturn(List.of());

      // When
      GetPublicHotelResult result = service.execute(HOTEL_ID);

      // Then
      assertEquals(List.of(), result.amenities());
    }

    @Test
    void shouldReturnNotFoundWhenHotelIsMissingOrNotPublished() {
      // Given
      // Repository chỉ trả hotel ACTIVE, nên hotel DRAFT và hotel không tồn tại đều là empty.
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE)).willReturn(Optional.empty());

      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(HOTEL_ID));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      assertEquals("Không tìm thấy khách sạn.", error.getMessage());
      then(amenities).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectRequestWhenHotelIdIsMissing() {
      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(null));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(hotels).shouldHaveNoInteractions();
    }
  }
}

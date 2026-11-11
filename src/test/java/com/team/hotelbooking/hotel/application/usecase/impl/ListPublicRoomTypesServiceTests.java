package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
@DisplayName("Loại phòng công khai của khách sạn")
class ListPublicRoomTypesServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");

  @Mock private HotelRepository hotels;

  @Mock private RoomTypeRepository roomTypes;

  @Mock private AmenityRepository amenities;

  private static final Amenity WIFI =
      new Amenity(
          UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a"),
          "FREE_WIFI",
          "Wi-Fi miễn phí",
          Amenity.Category.ROOM);

  private ListPublicRoomTypesService service;
  private Hotel activeHotel;
  private RoomType deluxe;

  @BeforeEach
  void setUp() {
    service = new ListPublicRoomTypesService(hotels, roomTypes, amenities);
    activeHotel =
        new Hotel(
            HOTEL_ID,
            UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9"),
            UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3"),
            "Khách sạn Biển Xanh",
            "khach-san-bien-xanh",
            null,
            "12 Võ Nguyên Giáp",
            null,
            null,
            null,
            null,
            null,
            LocalTime.of(14, 0),
            LocalTime.of(12, 0),
            Hotel.Status.ACTIVE,
            NOW,
            NOW,
            0L);
    deluxe =
        new RoomType(
            UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b"),
            HOTEL_ID,
            "Deluxe King",
            "Hướng biển",
            (short) 2,
            (short) 1,
            (short) 2,
            0L,
            true,
            NOW,
            NOW,
            4L);
  }

  @Nested
  @DisplayName("ListPublicRoomTypesUseCase.execute")
  class ListPublicRoomTypesTests {

    @Test
    void shouldReturnActiveRoomTypesWhenHotelIsActive() {
      // Given
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE))
          .willReturn(Optional.of(activeHotel));
      given(roomTypes.findActiveByHotelId(HOTEL_ID)).willReturn(List.of(deluxe));
      given(amenities.findByRoomTypeIds(List.of(deluxe.id())))
          .willReturn(Map.of(deluxe.id(), List.of(WIFI)));

      // When
      List<ListPublicRoomTypesResult> result = service.execute(HOTEL_ID);

      // Then
      assertEquals(
          List.of(
              new ListPublicRoomTypesResult(
                  deluxe.id(),
                  "Deluxe King",
                  "Hướng biển",
                  (short) 2,
                  (short) 1,
                  List.of(
                      new ListAmenitiesResult(WIFI.id(), "FREE_WIFI", "Wi-Fi miễn phí", "ROOM")))),
          result);
    }

    @Test
    void shouldReturnEmptyAmenitiesWhenRoomTypeHasNoAmenity() {
      // Given
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE))
          .willReturn(Optional.of(activeHotel));
      given(roomTypes.findActiveByHotelId(HOTEL_ID)).willReturn(List.of(deluxe));
      given(amenities.findByRoomTypeIds(List.of(deluxe.id()))).willReturn(Map.of());

      // When
      List<ListPublicRoomTypesResult> result = service.execute(HOTEL_ID);

      // Then
      assertEquals(1, result.size());
      assertEquals(List.of(), result.get(0).amenities());
    }

    @Test
    void shouldReturnEmptyListWhenHotelHasNoActiveRoomType() {
      // Given
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE))
          .willReturn(Optional.of(activeHotel));
      given(roomTypes.findActiveByHotelId(HOTEL_ID)).willReturn(List.of());

      // When
      List<ListPublicRoomTypesResult> result = service.execute(HOTEL_ID);

      // Then
      assertTrue(result.isEmpty());
      then(amenities).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnNotFoundWithoutReadingRoomTypesWhenHotelIsNotPublic() {
      // Given
      given(hotels.findByIdAndStatus(HOTEL_ID, Hotel.Status.ACTIVE)).willReturn(Optional.empty());

      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(HOTEL_ID));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectRequestWhenHotelIdIsMissing() {
      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(null));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(hotels).shouldHaveNoInteractions();
      then(roomTypes).shouldHaveNoInteractions();
    }
  }
}

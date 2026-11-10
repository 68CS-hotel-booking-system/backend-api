package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.command.ReplaceRoomTypeAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Thay danh sách tiện ích của loại phòng")
class ReplaceRoomTypeAmenitiesServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b");
  private static final UUID ADMIN_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
  private static final Amenity WIFI =
      new Amenity(
          UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a"),
          "FREE_WIFI",
          "Wi-Fi miễn phí",
          Amenity.Category.ROOM);
  private static final Amenity POOL =
      new Amenity(
          UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3"),
          "SWIMMING_POOL",
          "Hồ bơi",
          Amenity.Category.HOTEL);

  @Mock private HotelRepository hotels;

  @Mock private RoomTypeRepository roomTypes;

  @Mock private AmenityRepository amenities;

  private ReplaceRoomTypeAmenitiesService service;
  private Hotel hotel;
  private RoomType roomType;

  @BeforeEach
  void setUp() {
    service = new ReplaceRoomTypeAmenitiesService(hotels, roomTypes, amenities);
    hotel =
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
    roomType =
        new RoomType(
            ROOM_TYPE_ID,
            HOTEL_ID,
            "Deluxe King",
            null,
            (short) 2,
            (short) 1,
            (short) 2,
            0L,
            false,
            NOW,
            NOW,
            1L);
  }

  @Nested
  @DisplayName("ReplaceRoomTypeAmenitiesUseCase.execute")
  class ReplaceRoomTypeAmenitiesTests {

    @Test
    void shouldReplaceListAfterLockingRoomTypeWhenAmenitiesAreRoomCategory() {
      // Given
      givenAdminManagesHotel();
      given(roomTypes.findByIdAndHotelIdForUpdate(ROOM_TYPE_ID, HOTEL_ID))
          .willReturn(Optional.of(roomType));
      given(amenities.findAllByIds(Set.of(WIFI.id()))).willReturn(List.of(WIFI));
      given(amenities.findByRoomTypeId(ROOM_TYPE_ID)).willReturn(List.of(WIFI));

      // When
      List<ListAmenitiesResult> result =
          service.execute(
              new ReplaceRoomTypeAmenitiesCommand(
                  ADMIN_ID, true, HOTEL_ID, ROOM_TYPE_ID, List.of(WIFI.id())));

      // Then
      InOrder order = inOrder(roomTypes, amenities);
      order.verify(roomTypes).findByIdAndHotelIdForUpdate(ROOM_TYPE_ID, HOTEL_ID);
      order.verify(amenities).replaceRoomTypeAmenities(ROOM_TYPE_ID, Set.of(WIFI.id()));
      assertEquals(
          List.of(new ListAmenitiesResult(WIFI.id(), "FREE_WIFI", "Wi-Fi miễn phí", "ROOM")),
          result);
    }

    @Test
    void shouldRejectHotelAmenityWithoutWritingWhenCategoryIsWrong() {
      // Given
      givenAdminManagesHotel();
      given(roomTypes.findByIdAndHotelIdForUpdate(ROOM_TYPE_ID, HOTEL_ID))
          .willReturn(Optional.of(roomType));
      given(amenities.findAllByIds(Set.of(POOL.id()))).willReturn(List.of(POOL));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceRoomTypeAmenitiesCommand(
                          ADMIN_ID, true, HOTEL_ID, ROOM_TYPE_ID, List.of(POOL.id()))));

      // Then
      assertEquals(HotelErrorCode.AMENITY_CATEGORY_MISMATCH, error.code());
      assertEquals("Loại phòng chỉ được gắn tiện ích thuộc nhóm ROOM.", error.getMessage());
      then(amenities).should(never()).replaceRoomTypeAmenities(any(), any());
    }

    @Test
    void shouldReturnRoomTypeNotFoundWhenRoomTypeBelongsToAnotherHotel() {
      // Given
      givenAdminManagesHotel();
      given(roomTypes.findByIdAndHotelIdForUpdate(ROOM_TYPE_ID, HOTEL_ID))
          .willReturn(Optional.empty());

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceRoomTypeAmenitiesCommand(
                          ADMIN_ID, true, HOTEL_ID, ROOM_TYPE_ID, List.of(WIFI.id()))));

      // Then
      assertEquals(HotelErrorCode.ROOM_TYPE_NOT_FOUND, error.code());
      then(amenities).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectRequestWhenRoomTypeIdIsMissing() {
      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceRoomTypeAmenitiesCommand(
                          ADMIN_ID, true, HOTEL_ID, null, List.of())));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(hotels).shouldHaveNoInteractions();
      then(roomTypes).shouldHaveNoInteractions();
    }
  }

  private void givenAdminManagesHotel() {
    given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
  }
}

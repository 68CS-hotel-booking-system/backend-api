package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.command.ReplaceHotelAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
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
@DisplayName("Thay danh sách tiện ích của khách sạn")
class ReplaceHotelAmenitiesServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID OWNER_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
  private static final Amenity POOL =
      new Amenity(
          UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3"),
          "SWIMMING_POOL",
          "Hồ bơi",
          Amenity.Category.HOTEL);
  private static final Amenity PARKING =
      new Amenity(
          UUID.fromString("59fe1090-6e67-4d93-b140-232c5e885c27"),
          "FREE_PARKING",
          "Bãi đỗ xe miễn phí",
          Amenity.Category.HOTEL);
  private static final Amenity WIFI =
      new Amenity(
          UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a"),
          "FREE_WIFI",
          "Wi-Fi miễn phí",
          Amenity.Category.ROOM);

  @Mock private HotelRepository hotels;

  @Mock private AmenityRepository amenities;

  private ReplaceHotelAmenitiesService service;
  private Hotel hotel;

  @BeforeEach
  void setUp() {
    service = new ReplaceHotelAmenitiesService(hotels, amenities);
    hotel =
        new Hotel(
            HOTEL_ID,
            OWNER_ID,
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
            Hotel.Status.DRAFT,
            NOW,
            NOW,
            0L);
  }

  @Nested
  @DisplayName("ReplaceHotelAmenitiesUseCase.execute")
  class ReplaceHotelAmenitiesTests {

    @Test
    void shouldReplaceWholeListAfterLockingHotelWhenAmenitiesAreHotelCategory() {
      // Given
      givenOwnerManagesHotel();
      given(hotels.findByIdForUpdate(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(amenities.findAllByIds(Set.of(POOL.id(), PARKING.id())))
          .willReturn(List.of(POOL, PARKING));
      given(amenities.findByHotelId(HOTEL_ID)).willReturn(List.of(PARKING, POOL));

      // When
      List<ListAmenitiesResult> result =
          service.execute(
              new ReplaceHotelAmenitiesCommand(
                  OWNER_ID, false, HOTEL_ID, List.of(POOL.id(), PARKING.id(), POOL.id())));

      // Then
      InOrder order = inOrder(hotels, amenities);
      order.verify(hotels).findByIdForUpdate(HOTEL_ID);
      order.verify(amenities).replaceHotelAmenities(HOTEL_ID, Set.of(POOL.id(), PARKING.id()));
      assertEquals(
          List.of(
              new ListAmenitiesResult(PARKING.id(), "FREE_PARKING", "Bãi đỗ xe miễn phí", "HOTEL"),
              new ListAmenitiesResult(POOL.id(), "SWIMMING_POOL", "Hồ bơi", "HOTEL")),
          result);
    }

    @Test
    void shouldRemoveAllAmenitiesWhenListIsEmpty() {
      // Given
      givenOwnerManagesHotel();
      given(hotels.findByIdForUpdate(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(amenities.findAllByIds(Set.of())).willReturn(List.of());
      given(amenities.findByHotelId(HOTEL_ID)).willReturn(List.of());

      // When
      List<ListAmenitiesResult> result =
          service.execute(new ReplaceHotelAmenitiesCommand(OWNER_ID, false, HOTEL_ID, List.of()));

      // Then
      then(amenities).should().replaceHotelAmenities(HOTEL_ID, Set.of());
      assertEquals(List.of(), result);
    }

    @Test
    void shouldRejectRoomAmenityWithoutWritingWhenCategoryIsWrong() {
      // Given
      givenOwnerManagesHotel();
      given(hotels.findByIdForUpdate(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(amenities.findAllByIds(Set.of(POOL.id(), WIFI.id()))).willReturn(List.of(POOL, WIFI));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceHotelAmenitiesCommand(
                          OWNER_ID, false, HOTEL_ID, List.of(POOL.id(), WIFI.id()))));

      // Then
      assertEquals(HotelErrorCode.AMENITY_CATEGORY_MISMATCH, error.code());
      assertEquals("Khách sạn chỉ được gắn tiện ích thuộc nhóm HOTEL.", error.getMessage());
      then(amenities).should(never()).replaceHotelAmenities(any(), any());
    }

    @Test
    void shouldReturnAmenityNotFoundWithoutWritingWhenIdDoesNotExist() {
      // Given
      UUID missingId = UUID.fromString("00000000-0000-4000-8000-000000000001");
      givenOwnerManagesHotel();
      given(hotels.findByIdForUpdate(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(amenities.findAllByIds(Set.of(POOL.id(), missingId))).willReturn(List.of(POOL));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceHotelAmenitiesCommand(
                          OWNER_ID, false, HOTEL_ID, List.of(POOL.id(), missingId))));

      // Then
      assertEquals(HotelErrorCode.AMENITY_NOT_FOUND, error.code());
      then(amenities).should(never()).replaceHotelAmenities(any(), any());
    }

    @Test
    void shouldReturnNotFoundWithoutLockingWhenActorIsNotMemberOfHotel() {
      // Given
      UUID outsiderId = UUID.fromString("2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d");
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(HOTEL_ID, outsiderId)).willReturn(Set.of());

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceHotelAmenitiesCommand(
                          outsiderId, false, HOTEL_ID, List.of(POOL.id()))));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      then(hotels).should(never()).findByIdForUpdate(any());
      then(amenities).shouldHaveNoInteractions();
    }

    @Test
    void shouldDenyAccessWithoutLockingWhenActorIsReceptionistOfHotel() {
      // Given
      UUID receptionistId = UUID.fromString("5d6e7f8a-9b0c-4d1e-8f2a-3b4c5d6e7f80");
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(HOTEL_ID, receptionistId))
          .willReturn(Set.of(HotelMemberRole.RECEPTIONIST));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceHotelAmenitiesCommand(
                          receptionistId, false, HOTEL_ID, List.of(POOL.id()))));

      // Then
      assertEquals(HotelErrorCode.ACCESS_DENIED, error.code());
      then(hotels).should(never()).findByIdForUpdate(any());
      then(amenities).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectRequestWhenAmenityIdsAreMissing() {
      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceHotelAmenitiesCommand(OWNER_ID, false, HOTEL_ID, null)));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(hotels).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectRequestWhenAmenityIdsContainNull() {
      // Given
      List<UUID> ids = new ArrayList<>(Arrays.asList(POOL.id(), null));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ReplaceHotelAmenitiesCommand(OWNER_ID, false, HOTEL_ID, ids)));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals("Danh sách tiện ích không được chứa giá trị rỗng.", error.getMessage());
      then(hotels).shouldHaveNoInteractions();
    }
  }

  // Owner theo hotels.owner_user_id nên không cần tra hotel_members.
  private void givenOwnerManagesHotel() {
    given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
  }
}

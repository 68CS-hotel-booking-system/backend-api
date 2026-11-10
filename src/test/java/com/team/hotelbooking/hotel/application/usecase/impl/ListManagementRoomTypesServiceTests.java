package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomTypesQuery;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomTypesResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.RoomType;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Danh sách loại phòng quản lý")
class ListManagementRoomTypesServiceTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");

  @Mock private HotelRepository hotels;

  @Mock private RoomTypeRepository roomTypes;

  @Mock private IdentityAccounts accounts;

  private ListManagementRoomTypesService service;
  private ListManagementRoomTypesQuery query;
  private Hotel hotel;
  private RoomType suspendedRoomType;

  @BeforeEach
  void setUp() {
    service = new ListManagementRoomTypesService(hotels, roomTypes, accounts);
    query = new ListManagementRoomTypesQuery(ACTOR_ID, HOTEL_ID);
    hotel =
        new Hotel(
            HOTEL_ID,
            ACTOR_ID,
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
    suspendedRoomType =
        new RoomType(
            UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b"),
            HOTEL_ID,
            "Deluxe King",
            "Hướng biển",
            (short) 2,
            (short) 1,
            (short) 2,
            0L,
            false,
            NOW,
            NOW,
            3L);
  }

  private void givenActor(boolean active, boolean emailVerified, String... roles) {
    given(accounts.findById(ACTOR_ID))
        .willReturn(
            Optional.of(new AccountSnapshot(ACTOR_ID, active, emailVerified, Set.of(roles))));
  }

  @Nested
  @DisplayName("ListManagementRoomTypesUseCase.execute")
  class ListManagementRoomTypesTests {

    @Test
    void shouldReturnAllRoomTypesIncludingSuspendedWhenOwnerManagesHotel() {
      // Given
      givenActor(true, true, "HOTEL_OWNER");
      given(hotels.findByIdAndOwnerId(HOTEL_ID, ACTOR_ID)).willReturn(Optional.of(hotel));
      given(roomTypes.findByHotelId(HOTEL_ID)).willReturn(List.of(suspendedRoomType));

      // When
      List<ListManagementRoomTypesResult> result = service.execute(query);

      // Then
      assertEquals(
          List.of(
              new ListManagementRoomTypesResult(
                  suspendedRoomType.id(),
                  "Deluxe King",
                  "Hướng biển",
                  (short) 2,
                  (short) 1,
                  false,
                  NOW,
                  NOW,
                  3L)),
          result);
      assertFalse(result.get(0).active());
    }

    @Test
    void shouldReadAnyHotelWhenActorIsAdmin() {
      // Given
      givenActor(true, true, "ADMIN");
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(roomTypes.findByHotelId(HOTEL_ID)).willReturn(List.of());

      // When
      List<ListManagementRoomTypesResult> result = service.execute(query);

      // Then
      assertEquals(List.of(), result);
      then(hotels).should().findById(HOTEL_ID);
    }

    @Test
    void shouldReturnNotFoundWhenOwnerDoesNotOwnHotel() {
      // Given
      givenActor(true, true, "HOTEL_OWNER");
      given(hotels.findByIdAndOwnerId(HOTEL_ID, ACTOR_ID)).willReturn(Optional.empty());

      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(query));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    void shouldDenyAccessWhenActorHasNoManagementRole() {
      // Given
      givenActor(true, true, "CUSTOMER");

      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(query));

      // Then
      assertEquals(HotelErrorCode.ACCESS_DENIED, error.code());
      then(hotels).shouldHaveNoInteractions();
    }

    @Test
    void shouldDenyAccessWhenOwnerEmailIsNotVerified() {
      // Given
      givenActor(true, false, "HOTEL_OWNER");

      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(query));

      // Then
      assertEquals(HotelErrorCode.ACCESS_DENIED, error.code());
    }

    @Test
    void shouldDenyAccessWhenActorAccountIsMissing() {
      // Given
      given(accounts.findById(ACTOR_ID)).willReturn(Optional.empty());

      // When
      HotelException error = assertThrows(HotelException.class, () -> service.execute(query));

      // Then
      assertEquals(HotelErrorCode.ACCESS_DENIED, error.code());
    }

    @Test
    void shouldRejectRequestWhenHotelIdIsMissing() {
      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () -> service.execute(new ListManagementRoomTypesQuery(ACTOR_ID, null)));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(accounts).shouldHaveNoInteractions();
    }
  }
}

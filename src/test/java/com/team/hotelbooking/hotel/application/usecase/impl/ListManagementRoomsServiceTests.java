package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Danh sách phòng vật lý quản lý")
class ListManagementRoomsServiceTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b");
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
  private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

  @Mock private HotelRepository hotels;

  @Mock private RoomRepository rooms;

  @Mock private IdentityAccounts accounts;

  private ListManagementRoomsService service;
  private Hotel hotel;
  private Room room;

  @BeforeEach
  void setUp() {
    service = new ListManagementRoomsService(hotels, rooms, accounts, 100);
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
    room =
        new Room(
            UUID.fromString("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"),
            HOTEL_ID,
            ROOM_TYPE_ID,
            "101",
            "1",
            Room.Readiness.DIRTY,
            NOW,
            null,
            NOW,
            null,
            NOW,
            NOW,
            2L);
  }

  private void givenOwnerOfHotel() {
    given(accounts.findById(ACTOR_ID))
        .willReturn(Optional.of(new AccountSnapshot(ACTOR_ID, true, true, Set.of("HOTEL_OWNER"))));
    given(hotels.findByIdAndOwnerId(HOTEL_ID, ACTOR_ID)).willReturn(Optional.of(hotel));
  }

  private static PageResult<Room> pageOf(List<Room> content) {
    return new PageResult<>(content, 0, 20, content.size(), 1, true, true);
  }

  @Nested
  @DisplayName("ListManagementRoomsUseCase.execute")
  class ListManagementRoomsTests {

    @Test
    void shouldReturnRoomsOfHotelWhenRoomTypeFilterIsMissing() {
      // Given
      givenOwnerOfHotel();
      given(rooms.findByHotelId(HOTEL_ID, FIRST_PAGE)).willReturn(pageOf(List.of(room)));

      // When
      PageResult<ListManagementRoomsResult> result =
          service.execute(new ListManagementRoomsQuery(ACTOR_ID, HOTEL_ID, null, FIRST_PAGE));

      // Then
      assertEquals(
          List.of(
              new ListManagementRoomsResult(
                  room.id(), ROOM_TYPE_ID, "101", "1", "DIRTY", NOW, NOW, null, NOW, NOW, 2L)),
          result.content());
      assertEquals(1, result.totalElements());
    }

    @Test
    void shouldFilterInsideRequestedHotelWhenRoomTypeIdIsProvided() {
      // Given
      givenOwnerOfHotel();
      given(rooms.findByHotelIdAndRoomTypeId(HOTEL_ID, ROOM_TYPE_ID, FIRST_PAGE))
          .willReturn(pageOf(List.of()));

      // When
      PageResult<ListManagementRoomsResult> result =
          service.execute(
              new ListManagementRoomsQuery(ACTOR_ID, HOTEL_ID, ROOM_TYPE_ID, FIRST_PAGE));

      // Then
      assertEquals(List.of(), result.content());
      then(rooms).should().findByHotelIdAndRoomTypeId(HOTEL_ID, ROOM_TYPE_ID, FIRST_PAGE);
    }

    @Test
    void shouldReturnNotFoundWhenOwnerDoesNotOwnHotel() {
      // Given
      given(accounts.findById(ACTOR_ID))
          .willReturn(
              Optional.of(new AccountSnapshot(ACTOR_ID, true, true, Set.of("HOTEL_OWNER"))));
      given(hotels.findByIdAndOwnerId(HOTEL_ID, ACTOR_ID)).willReturn(Optional.empty());

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ListManagementRoomsQuery(ACTOR_ID, HOTEL_ID, null, FIRST_PAGE)));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      then(rooms).shouldHaveNoInteractions();
    }

    @Test
    void shouldDenyAccessWhenActorIsInactive() {
      // Given
      given(accounts.findById(ACTOR_ID))
          .willReturn(Optional.of(new AccountSnapshot(ACTOR_ID, false, true, Set.of("ADMIN"))));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ListManagementRoomsQuery(ACTOR_ID, HOTEL_ID, null, FIRST_PAGE)));

      // Then
      assertEquals(HotelErrorCode.ACCESS_DENIED, error.code());
      then(hotels).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    void shouldRejectRequestWhenPaginationIsOutOfRange(int page, int size) {
      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  service.execute(
                      new ListManagementRoomsQuery(
                          ACTOR_ID, HOTEL_ID, null, new PageQuery(page, size))));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(accounts).shouldHaveNoInteractions();
    }
  }
}

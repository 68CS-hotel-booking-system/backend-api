package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.UpdateRoomCommand;
import com.team.hotelbooking.hotel.application.result.RoomResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ cập nhật phòng vật lý (HT21)")
class UpdateRoomServiceTests {

  private static final Instant CREATED_AT = Instant.parse("2026-10-01T00:00:00Z");
  private static final UUID OWNER_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID OTHER_USER_ID = UUID.fromString("2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d");
  private static final UUID READINESS_ACTOR = UUID.randomUUID();

  @Mock private HotelRepository hotels;
  @Mock private RoomRepository rooms;
  @Mock private com.team.hotelbooking.hotel.application.port.RoomInventoryPort inventory;

  private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);

  private UpdateRoomService service;

  private UUID hotelId;
  private Hotel hotel;
  private UUID roomId;
  private UUID roomTypeId;
  private Room existingRoom;

  @BeforeEach
  void setUp() {
    service = new UpdateRoomService(hotels, rooms, clock, inventory);
    hotelId = UUID.randomUUID();
    roomId = UUID.randomUUID();
    roomTypeId = UUID.randomUUID();

    hotel =
        new Hotel(
            hotelId,
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
            Hotel.Status.ACTIVE,
            CREATED_AT,
            CREATED_AT,
            0L);

    existingRoom = room("P.101", "1", Room.Readiness.READY, null);
  }

  private Room room(
      String roomNumber, String floor, Room.Readiness readiness, Instant operationalUntil) {
    return new Room(
        roomId,
        hotelId,
        roomTypeId,
        roomNumber,
        floor,
        readiness,
        CREATED_AT,
        READINESS_ACTOR,
        CREATED_AT,
        operationalUntil,
        CREATED_AT,
        CREATED_AT,
        7L);
  }

  private void givenExistingRoom() {
    given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
    given(rooms.findByIdAndHotelId(roomId, hotelId)).willReturn(Optional.of(existingRoom));
  }

  private void givenUpdateReturnsSavedRoom() {
    given(rooms.update(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void shouldRejectDeactivationWhenInventoryCannotServeExistingBookings() {
    givenExistingRoom();
    givenUpdateReturnsSavedRoom();
    org.mockito.BDDMockito.willThrow(new HotelException(HotelErrorCode.ROOM_OPERATION_CONFLICT))
        .given(inventory)
        .requireAllocatableAfterDeactivation(roomTypeId, roomId);
    assertThatThrownBy(
            () ->
                service.execute(
                    new UpdateRoomCommand(
                        OWNER_ID, false, hotelId, roomId, null, null, null, false)))
        .isInstanceOf(HotelException.class)
        .extracting(UpdateRoomServiceTests::errorCodeOf)
        .isEqualTo(HotelErrorCode.ROOM_OPERATION_CONFLICT);
    then(inventory).should().lockRoomType(hotelId, roomTypeId);
    then(inventory).should().requireAllocatableAfterDeactivation(roomTypeId, roomId);
  }

  private static HotelErrorCode errorCodeOf(Throwable exception) {
    return ((HotelException) exception).getErrorCode();
  }

  @Nested
  @DisplayName("execute")
  class ExecuteTests {

    @Test
    @DisplayName("Cập nhật thành công thông tin phòng khi dữ liệu hợp lệ")
    void shouldUpdateRoomSuccessfully() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(
              OWNER_ID, false, hotelId, roomId, " P.102 ", (short) 2, "cleaning", null);
      givenExistingRoom();
      given(rooms.existsOtherRoomWithNumber(hotelId, "P.102", roomId)).willReturn(false);
      givenUpdateReturnsSavedRoom();

      // When
      RoomResult result = service.execute(command);

      // Then
      assertThat(result.id()).isEqualTo(roomId);
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.roomTypeId()).isEqualTo(roomTypeId);
      assertThat(result.roomNumber()).isEqualTo("P.102");
      assertThat(result.floor()).isEqualTo((short) 2);
      assertThat(result.status()).isEqualTo("cleaning");
      assertThat(result.active()).isTrue();
      assertThat(result.createdAt()).isEqualTo(CREATED_AT);
      assertThat(result.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("Lưu DIRTY cho maintenance, giữ mốc readiness và các field không gửi")
    void shouldStoreDirtyAndKeepOtherFieldsWhenStatusIsMaintenance() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(
              OWNER_ID, false, hotelId, roomId, null, null, " MAINTENANCE ", null);
      givenExistingRoom();
      givenUpdateReturnsSavedRoom();
      ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);

      // When
      RoomResult result = service.execute(command);

      // Then
      then(rooms).should().update(saved.capture());
      assertThat(saved.getValue())
          .isEqualTo(
              new Room(
                  roomId,
                  hotelId,
                  roomTypeId,
                  "P.101",
                  "1",
                  Room.Readiness.DIRTY,
                  CREATED_AT,
                  READINESS_ACTOR,
                  CREATED_AT,
                  null,
                  CREATED_AT,
                  clock.instant(),
                  7L));
      assertThat(result.status()).isEqualTo("cleaning");
    }

    @Test
    @DisplayName("Không kiểm trùng và giữ số phòng cũ khi số mới chỉ khác hoa thường")
    void shouldKeepRoomNumberWhenNewNumberOnlyDiffersInCase() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, "p.101", null, null, null);
      givenExistingRoom();
      givenUpdateReturnsSavedRoom();

      // When
      RoomResult result = service.execute(command);

      // Then
      assertThat(result.roomNumber()).isEqualTo("P.101");
      then(rooms).should().findByIdAndHotelId(roomId, hotelId);
      then(rooms).should().update(any(Room.class));
      then(rooms).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Ngừng sử dụng phòng thành công khi active được đặt thành false")
    void shouldDeactivateRoomSuccessfully() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, null, null, null, false);
      givenExistingRoom();
      givenUpdateReturnsSavedRoom();
      ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);

      // When
      RoomResult result = service.execute(command);

      // Then
      then(rooms).should().update(saved.capture());
      assertThat(saved.getValue().operationalUntil()).isEqualTo(clock.instant());
      assertThat(result.active()).isFalse();
      assertThat(result.roomNumber()).isEqualTo("P.101");
    }

    @Test
    @DisplayName("Bật lại phòng thì xóa mốc kết thúc phục vụ")
    void shouldClearOperationalUntilWhenRoomIsReactivated() {
      // Given
      existingRoom = room("P.101", "Tầng trệt", Room.Readiness.READY, CREATED_AT);
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, null, null, null, true);
      givenExistingRoom();
      givenUpdateReturnsSavedRoom();

      // When
      RoomResult result = service.execute(command);

      // Then
      assertThat(result.active()).isTrue();
      assertThat(result.floor()).isNull();
      assertThat(result.status()).isEqualTo("ready");
    }

    @Test
    @DisplayName("Ném lỗi HOTEL_NOT_FOUND khi khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelDoesNotExist() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, "P.102", null, null, null);
      given(hotels.findById(hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Không tìm thấy khách sạn.")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
      then(rooms).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném lỗi ROOM_NOT_FOUND khi phòng không thuộc khách sạn")
    void shouldThrowRoomNotFoundWhenRoomDoesNotExist() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, "P.102", null, null, null);
      given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
      given(rooms.findByIdAndHotelId(roomId, hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("Ném lỗi DUPLICATE_ROOM_NUMBER khi số phòng bị trùng lặp trong khách sạn")
    void shouldThrowDuplicateRoomNumberWhenNumberAlreadyExists() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, "P.201", null, null, null);
      givenExistingRoom();
      given(rooms.existsOtherRoomWithNumber(hotelId, "P.201", roomId)).willReturn(true);

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Số phòng 'P.201' đã tồn tại trong khách sạn này.")
          .satisfies(
              ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.DUPLICATE_ROOM_NUMBER));
      then(rooms).should().findByIdAndHotelId(roomId, hotelId);
      then(rooms).should().existsOtherRoomWithNumber(hotelId, "P.201", roomId);
      then(rooms).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Ném lỗi INVALID_REQUEST khi số phòng rỗng")
    void shouldThrowInvalidRequestWhenRoomNumberIsBlank() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(OWNER_ID, false, hotelId, roomId, "   ", null, null, null);
      givenExistingRoom();

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Số phòng không được để trống.")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.INVALID_REQUEST));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_REQUEST khi trạng thái buồng phòng không hợp lệ")
    void shouldThrowInvalidRequestWhenStatusIsInvalid() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(
              OWNER_ID, false, hotelId, roomId, null, null, "invalid_status", null);
      givenExistingRoom();

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage(
              "Trạng thái buồng phòng không hợp lệ. Chỉ chấp nhận: ready, cleaning, maintenance.")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.INVALID_REQUEST));
      then(rooms).should().findByIdAndHotelId(roomId, hotelId);
      then(rooms).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("Phân quyền theo khách sạn")
  class AccessTests {

    private UpdateRoomCommand commandBy(UUID actorUserId, boolean admin) {
      return new UpdateRoomCommand(
          actorUserId, admin, hotelId, roomId, null, (short) 5, null, null);
    }

    @Test
    @DisplayName("HOTEL_MANAGER đang hoạt động của đúng khách sạn được sửa phòng")
    void shouldUpdateRoomWhenActorIsActiveHotelManager() {
      // Given
      givenExistingRoom();
      given(hotels.findActiveMemberRoles(hotelId, OTHER_USER_ID))
          .willReturn(Set.of(HotelMemberRole.HOTEL_MANAGER));
      givenUpdateReturnsSavedRoom();

      // When
      RoomResult result = service.execute(commandBy(OTHER_USER_ID, false));

      // Then
      assertThat(result.floor()).isEqualTo((short) 5);
    }

    @Test
    @DisplayName("ADMIN sửa được phòng mà không cần là thành viên khách sạn")
    void shouldUpdateRoomWithoutMembershipLookupWhenActorIsAdmin() {
      // Given
      givenExistingRoom();
      givenUpdateReturnsSavedRoom();

      // When
      service.execute(commandBy(OTHER_USER_ID, true));

      // Then
      then(hotels).should().findById(hotelId);
      then(hotels).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Ném ACCESS_DENIED và không đọc phòng khi người gọi là RECEPTIONIST")
    void shouldThrowAccessDeniedWhenActorIsReceptionist() {
      // Given
      given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(hotelId, OTHER_USER_ID))
          .willReturn(Set.of(HotelMemberRole.RECEPTIONIST));

      // When & Then
      assertThatThrownBy(() -> service.execute(commandBy(OTHER_USER_ID, false)))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.ACCESS_DENIED));
      then(rooms).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_NOT_FOUND khi người gọi không thuộc khách sạn")
    void shouldThrowHotelNotFoundWhenActorIsNotHotelMember() {
      // Given
      given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(hotelId, OTHER_USER_ID)).willReturn(Set.of());

      // When & Then
      assertThatThrownBy(() -> service.execute(commandBy(OTHER_USER_ID, false)))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
      then(rooms).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_NOT_FOUND khi ADMIN sửa phòng của khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenAdminTargetsMissingHotel() {
      // Given
      given(hotels.findById(hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(commandBy(OTHER_USER_ID, true)))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
      then(rooms).shouldHaveNoInteractions();
    }
  }
}

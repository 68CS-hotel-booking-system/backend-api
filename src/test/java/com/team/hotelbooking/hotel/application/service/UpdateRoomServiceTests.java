package com.team.hotelbooking.hotel.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.UpdateRoomCommand;
import com.team.hotelbooking.hotel.application.result.RoomResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
@DisplayName("Dịch vụ cập nhật phòng vật lý (HT21)")
class UpdateRoomServiceTests {

  @Mock private HotelJpaRepository hotelJpaRepository;
  @Mock private RoomJpaRepository roomJpaRepository;

  private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);

  private UpdateRoomService service;

  private UUID hotelId;
  private UUID roomId;
  private UUID roomTypeId;
  private RoomJpaEntity existingRoom;

  @BeforeEach
  void setUp() {
    service = new UpdateRoomService(hotelJpaRepository, roomJpaRepository, clock);
    hotelId = UUID.randomUUID();
    roomId = UUID.randomUUID();
    roomTypeId = UUID.randomUUID();

    existingRoom = new RoomJpaEntity();
    existingRoom.setId(roomId);
    existingRoom.setHotelId(hotelId);
    existingRoom.setRoomTypeId(roomTypeId);
    existingRoom.setRoomNumber("P.101");
    existingRoom.setFloor("1");
    existingRoom.setStatus(RoomJpaEntity.DbRoomStatus.ready);
    existingRoom.setActive(true);
    existingRoom.setCreatedAt(Instant.parse("2026-10-01T00:00:00Z"));
    existingRoom.setUpdatedAt(Instant.parse("2026-10-01T00:00:00Z"));
  }

  @Nested
  @DisplayName("execute")
  class ExecuteTests {

    @Test
    @DisplayName("Cập nhật thành công thông tin phòng khi dữ liệu hợp lệ")
    void shouldUpdateRoomSuccessfully() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(hotelId, roomId, "P.102", (short) 1, "cleaning", null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomJpaRepository.findByIdAndHotelId(roomId, hotelId))
          .willReturn(Optional.of(existingRoom));
      given(roomJpaRepository.existsByHotelIdAndRoomNumberAndIdNot(hotelId, "P.102", roomId))
          .willReturn(false);
      given(roomJpaRepository.save(any(RoomJpaEntity.class))).willAnswer(inv -> inv.getArgument(0));

      // When
      RoomResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(roomId);
      assertThat(result.roomNumber()).isEqualTo("P.102");
      assertThat(result.status()).isEqualTo("cleaning");
      assertThat(result.updatedAt()).isEqualTo(clock.instant());

      then(roomJpaRepository).should().save(existingRoom);
    }

    @Test
    @DisplayName("Ngừng sử dụng phòng thành công khi active được đặt thành false")
    void shouldDeactivateRoomSuccessfully() {
      // Given
      UpdateRoomCommand command = new UpdateRoomCommand(hotelId, roomId, null, null, null, false);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomJpaRepository.findByIdAndHotelId(roomId, hotelId))
          .willReturn(Optional.of(existingRoom));
      given(roomJpaRepository.save(any(RoomJpaEntity.class))).willAnswer(inv -> inv.getArgument(0));

      // When
      RoomResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.active()).isFalse();
      assertThat(result.roomNumber()).isEqualTo("P.101");
    }

    @Test
    @DisplayName("Ném lỗi HOTEL_NOT_FOUND khi khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelDoesNotExist() {
      // Given
      UpdateRoomCommand command = new UpdateRoomCommand(hotelId, roomId, "P.102", null, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(false);

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
    }

    @Test
    @DisplayName("Ném lỗi ROOM_NOT_FOUND khi phòng không thuộc khách sạn")
    void shouldThrowRoomNotFoundWhenRoomDoesNotExist() {
      // Given
      UpdateRoomCommand command = new UpdateRoomCommand(hotelId, roomId, "P.102", null, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomJpaRepository.findByIdAndHotelId(roomId, hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("Ném lỗi DUPLICATE_ROOM_NUMBER khi số phòng bị trùng lặp trong khách sạn")
    void shouldThrowDuplicateRoomNumberWhenNumberAlreadyExists() {
      // Given
      UpdateRoomCommand command = new UpdateRoomCommand(hotelId, roomId, "P.201", null, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomJpaRepository.findByIdAndHotelId(roomId, hotelId))
          .willReturn(Optional.of(existingRoom));
      given(roomJpaRepository.existsByHotelIdAndRoomNumberAndIdNot(hotelId, "P.201", roomId))
          .willReturn(true);

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.DUPLICATE_ROOM_NUMBER));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_REQUEST khi số phòng rỗng")
    void shouldThrowInvalidRequestWhenRoomNumberIsBlank() {
      // Given
      UpdateRoomCommand command = new UpdateRoomCommand(hotelId, roomId, "   ", null, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomJpaRepository.findByIdAndHotelId(roomId, hotelId))
          .willReturn(Optional.of(existingRoom));

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.INVALID_REQUEST));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_REQUEST khi trạng thái buồng phòng không hợp lệ")
    void shouldThrowInvalidRequestWhenStatusIsInvalid() {
      // Given
      UpdateRoomCommand command =
          new UpdateRoomCommand(hotelId, roomId, null, null, "invalid_status", null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomJpaRepository.findByIdAndHotelId(roomId, hotelId))
          .willReturn(Optional.of(existingRoom));

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.INVALID_REQUEST));
    }
  }
}

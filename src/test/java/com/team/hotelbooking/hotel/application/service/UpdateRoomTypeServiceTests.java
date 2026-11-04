package com.team.hotelbooking.hotel.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.UpdateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
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
@DisplayName("Dịch vụ cập nhật loại phòng (HT17)")
class UpdateRoomTypeServiceTests {

  @Mock private HotelJpaRepository hotelJpaRepository;
  @Mock private RoomTypeJpaRepository roomTypeJpaRepository;

  private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);

  private UpdateRoomTypeService service;

  private UUID hotelId;
  private UUID roomTypeId;
  private RoomTypeJpaEntity existingRoomType;

  @BeforeEach
  void setUp() {
    service = new UpdateRoomTypeService(hotelJpaRepository, roomTypeJpaRepository, clock);
    hotelId = UUID.randomUUID();
    roomTypeId = UUID.randomUUID();

    existingRoomType = new RoomTypeJpaEntity();
    existingRoomType.setId(roomTypeId);
    existingRoomType.setHotelId(hotelId);
    existingRoomType.setName("Phòng Tiêu Chuẩn");
    existingRoomType.setDescription("Mô tả cũ");
    existingRoomType.setMaxAdults((short) 2);
    existingRoomType.setMaxChildren((short) 1);
    existingRoomType.setMaxOccupancy((short) 2);
    existingRoomType.setBasePrice(1_000_000L);
    existingRoomType.setActive(true);
    existingRoomType.setCreatedAt(Instant.parse("2026-10-01T00:00:00Z"));
    existingRoomType.setUpdatedAt(Instant.parse("2026-10-01T00:00:00Z"));
  }

  @Nested
  @DisplayName("execute")
  class ExecuteTests {

    @Test
    @DisplayName("Cập nhật thành công thông tin loại phòng khi dữ liệu hợp lệ")
    void shouldUpdateRoomTypeSuccessfully() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              hotelId,
              roomTypeId,
              "Phòng Deluxe Hướng Biển",
              "Mô tả mới có ban công",
              (short) 3,
              (short) 2,
              (short) 4,
              1_800_000L,
              null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomTypeJpaRepository.findByIdAndHotelId(roomTypeId, hotelId))
          .willReturn(Optional.of(existingRoomType));
      given(roomTypeJpaRepository.save(any(RoomTypeJpaEntity.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // When
      RoomTypeResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(roomTypeId);
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.name()).isEqualTo("Phòng Deluxe Hướng Biển");
      assertThat(result.description()).isEqualTo("Mô tả mới có ban công");
      assertThat(result.maxAdults()).isEqualTo((short) 3);
      assertThat(result.maxChildren()).isEqualTo((short) 2);
      assertThat(result.maxOccupancy()).isEqualTo((short) 4);
      assertThat(result.basePrice()).isEqualTo(1_800_000L);
      assertThat(result.active()).isTrue();
      assertThat(result.updatedAt()).isEqualTo(clock.instant());

      then(roomTypeJpaRepository).should().save(existingRoomType);
    }

    @Test
    @DisplayName("Ngừng bán loại phòng thành công khi active được đặt thành false")
    void shouldSuspendRoomTypeSuccessfully() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(hotelId, roomTypeId, null, null, null, null, null, null, false);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomTypeJpaRepository.findByIdAndHotelId(roomTypeId, hotelId))
          .willReturn(Optional.of(existingRoomType));
      given(roomTypeJpaRepository.save(any(RoomTypeJpaEntity.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // When
      RoomTypeResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.active()).isFalse();
      assertThat(result.name()).isEqualTo("Phòng Tiêu Chuẩn");
    }

    @Test
    @DisplayName("Ném lỗi HOTEL_NOT_FOUND khi khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelDoesNotExist() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              hotelId, roomTypeId, "Tên mới", null, null, null, null, null, null);

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
    @DisplayName("Ném lỗi ROOM_TYPE_NOT_FOUND khi loại phòng không thuộc khách sạn")
    void shouldThrowRoomTypeNotFoundWhenRoomTypeDoesNotExist() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              hotelId, roomTypeId, "Tên mới", null, null, null, null, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomTypeJpaRepository.findByIdAndHotelId(roomTypeId, hotelId))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.ROOM_TYPE_NOT_FOUND));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_REQUEST khi tên loại phòng rỗng hoặc chỉ toàn khoảng trắng")
    void shouldThrowInvalidRequestWhenNameIsBlank() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(hotelId, roomTypeId, "   ", null, null, null, null, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomTypeJpaRepository.findByIdAndHotelId(roomTypeId, hotelId))
          .willReturn(Optional.of(existingRoomType));

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.INVALID_REQUEST));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_OCCUPANCY khi số người lớn vượt quá sức chứa tối đa")
    void shouldThrowInvalidOccupancyWhenAdultsExceedOccupancy() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              hotelId, roomTypeId, null, null, (short) 4, null, (short) 2, null, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomTypeJpaRepository.findByIdAndHotelId(roomTypeId, hotelId))
          .willReturn(Optional.of(existingRoomType));

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.INVALID_OCCUPANCY));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_PRICE khi giá cơ bản nhỏ hơn 0")
    void shouldThrowInvalidPriceWhenBasePriceIsNegative() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              hotelId, roomTypeId, null, null, null, null, null, -50_000L, null);

      given(hotelJpaRepository.existsById(hotelId)).willReturn(true);
      given(roomTypeJpaRepository.findByIdAndHotelId(roomTypeId, hotelId))
          .willReturn(Optional.of(existingRoomType));

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex ->
                  assertThat(((HotelException) ex).getErrorCode())
                      .isEqualTo(HotelErrorCode.INVALID_PRICE));
    }
  }
}

package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.UpdateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
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
@DisplayName("Dịch vụ cập nhật loại phòng (HT17)")
class UpdateRoomTypeServiceTests {

  private static final Instant CREATED_AT = Instant.parse("2026-10-01T00:00:00Z");
  private static final UUID OWNER_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID OTHER_USER_ID = UUID.fromString("2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d");

  @Mock private HotelRepository hotels;
  @Mock private RoomTypeRepository roomTypes;

  private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);

  private UpdateRoomTypeService service;

  private UUID hotelId;
  private Hotel hotel;
  private UUID roomTypeId;
  private RoomType existingRoomType;

  @BeforeEach
  void setUp() {
    service = new UpdateRoomTypeService(hotels, roomTypes, clock);
    hotelId = UUID.randomUUID();
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

    existingRoomType =
        new RoomType(
            roomTypeId,
            hotelId,
            "Phòng Tiêu Chuẩn",
            "Mô tả cũ",
            (short) 2,
            (short) 1,
            (short) 2,
            1_000_000L,
            true,
            CREATED_AT,
            CREATED_AT,
            5L);
  }

  private void givenExistingRoomType() {
    given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
    given(roomTypes.findByIdAndHotelId(roomTypeId, hotelId))
        .willReturn(Optional.of(existingRoomType));
  }

  private void givenUpdateReturnsSavedRoomType() {
    given(roomTypes.update(any(RoomType.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  private static HotelErrorCode errorCodeOf(Throwable exception) {
    return ((HotelException) exception).getErrorCode();
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
              OWNER_ID,
              false,
              hotelId,
              roomTypeId,
              "  Phòng Deluxe Hướng Biển  ",
              "  Mô tả mới có ban công  ",
              (short) 3,
              (short) 2,
              (short) 4,
              1_800_000L,
              null);
      givenExistingRoomType();
      givenUpdateReturnsSavedRoomType();

      // When
      RoomTypeResult result = service.execute(command);

      // Then
      assertThat(result.id()).isEqualTo(roomTypeId);
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.name()).isEqualTo("Phòng Deluxe Hướng Biển");
      assertThat(result.description()).isEqualTo("Mô tả mới có ban công");
      assertThat(result.maxAdults()).isEqualTo((short) 3);
      assertThat(result.maxChildren()).isEqualTo((short) 2);
      assertThat(result.maxOccupancy()).isEqualTo((short) 4);
      assertThat(result.basePrice()).isEqualTo(1_800_000L);
      assertThat(result.active()).isTrue();
      assertThat(result.createdAt()).isEqualTo(CREATED_AT);
      assertThat(result.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("Giữ nguyên field không gửi cùng id, createdAt và version khi lưu")
    void shouldKeepUnchangedFieldsWhenCommandOnlyHasName() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, "Tên mới", null, null, null, null, null, null);
      givenExistingRoomType();
      givenUpdateReturnsSavedRoomType();
      ArgumentCaptor<RoomType> saved = ArgumentCaptor.forClass(RoomType.class);

      // When
      service.execute(command);

      // Then
      then(roomTypes).should().update(saved.capture());
      assertThat(saved.getValue())
          .isEqualTo(
              new RoomType(
                  roomTypeId,
                  hotelId,
                  "Tên mới",
                  "Mô tả cũ",
                  (short) 2,
                  (short) 1,
                  (short) 2,
                  1_000_000L,
                  true,
                  CREATED_AT,
                  clock.instant(),
                  5L));
    }

    @Test
    @DisplayName("Ngừng bán loại phòng thành công khi active được đặt thành false")
    void shouldSuspendRoomTypeSuccessfully() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, null, null, null, null, null, null, false);
      givenExistingRoomType();
      givenUpdateReturnsSavedRoomType();

      // When
      RoomTypeResult result = service.execute(command);

      // Then
      assertThat(result.active()).isFalse();
      assertThat(result.name()).isEqualTo("Phòng Tiêu Chuẩn");
    }

    @Test
    @DisplayName("Ném lỗi HOTEL_NOT_FOUND khi khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelDoesNotExist() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, "Tên mới", null, null, null, null, null, null);
      given(hotels.findById(hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Không tìm thấy khách sạn.")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném lỗi ROOM_TYPE_NOT_FOUND khi loại phòng không thuộc khách sạn")
    void shouldThrowRoomTypeNotFoundWhenRoomTypeDoesNotExist() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, "Tên mới", null, null, null, null, null, null);
      given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
      given(roomTypes.findByIdAndHotelId(roomTypeId, hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.ROOM_TYPE_NOT_FOUND));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_REQUEST khi tên loại phòng rỗng hoặc chỉ toàn khoảng trắng")
    void shouldThrowInvalidRequestWhenNameIsBlank() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, "   ", null, null, null, null, null, null);
      givenExistingRoomType();

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Tên loại phòng không được để trống.")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.INVALID_REQUEST));
      then(roomTypes).should().findByIdAndHotelId(roomTypeId, hotelId);
      then(roomTypes).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Ném lỗi INVALID_OCCUPANCY khi số người lớn vượt quá sức chứa tối đa")
    void shouldThrowInvalidOccupancyWhenAdultsExceedOccupancy() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID,
              false,
              hotelId,
              roomTypeId,
              null,
              null,
              (short) 4,
              null,
              (short) 2,
              null,
              null);
      givenExistingRoomType();

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Số người lớn (4) không được vượt quá sức chứa tối đa (2).")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.INVALID_OCCUPANCY));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_OCCUPANCY khi chỉ tăng người lớn vượt sức chứa hiện tại")
    void shouldThrowInvalidOccupancyWhenAdultsExceedCurrentOccupancy() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, null, null, (short) 3, null, null, null, null);
      givenExistingRoomType();

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.INVALID_OCCUPANCY));
    }

    @Test
    @DisplayName("Ném lỗi INVALID_PRICE khi giá cơ bản nhỏ hơn 0")
    void shouldThrowInvalidPriceWhenBasePriceIsNegative() {
      // Given
      UpdateRoomTypeCommand command =
          new UpdateRoomTypeCommand(
              OWNER_ID, false, hotelId, roomTypeId, null, null, null, null, null, -50_000L, null);
      givenExistingRoomType();

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(HotelException.class)
          .hasMessage("Giá cơ bản phải lớn hơn hoặc bằng 0.")
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.INVALID_PRICE));
    }
  }

  @Nested
  @DisplayName("Phân quyền theo khách sạn")
  class AccessTests {

    private UpdateRoomTypeCommand commandBy(UUID actorUserId, boolean admin) {
      return new UpdateRoomTypeCommand(
          actorUserId, admin, hotelId, roomTypeId, "Tên mới", null, null, null, null, null, null);
    }

    @Test
    @DisplayName("HOTEL_MANAGER đang hoạt động của đúng khách sạn được sửa loại phòng")
    void shouldUpdateRoomTypeWhenActorIsActiveHotelManager() {
      // Given
      givenExistingRoomType();
      given(hotels.findActiveMemberRoles(hotelId, OTHER_USER_ID))
          .willReturn(Set.of(HotelMemberRole.HOTEL_MANAGER));
      givenUpdateReturnsSavedRoomType();

      // When
      RoomTypeResult result = service.execute(commandBy(OTHER_USER_ID, false));

      // Then
      assertThat(result.name()).isEqualTo("Tên mới");
    }

    @Test
    @DisplayName("ADMIN sửa được loại phòng mà không cần là thành viên khách sạn")
    void shouldUpdateRoomTypeWithoutMembershipLookupWhenActorIsAdmin() {
      // Given
      givenExistingRoomType();
      givenUpdateReturnsSavedRoomType();

      // When
      service.execute(commandBy(OTHER_USER_ID, true));

      // Then
      then(hotels).should().findById(hotelId);
      then(hotels).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Ném ACCESS_DENIED và không đọc loại phòng khi người gọi là RECEPTIONIST")
    void shouldThrowAccessDeniedWhenActorIsReceptionist() {
      // Given
      given(hotels.findById(hotelId)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(hotelId, OTHER_USER_ID))
          .willReturn(Set.of(HotelMemberRole.RECEPTIONIST));

      // When & Then
      assertThatThrownBy(() -> service.execute(commandBy(OTHER_USER_ID, false)))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.ACCESS_DENIED));
      then(roomTypes).shouldHaveNoInteractions();
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
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_NOT_FOUND khi ADMIN sửa loại phòng của khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenAdminTargetsMissingHotel() {
      // Given
      given(hotels.findById(hotelId)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(commandBy(OTHER_USER_ID, true)))
          .isInstanceOf(HotelException.class)
          .satisfies(ex -> assertThat(errorCodeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
      then(roomTypes).shouldHaveNoInteractions();
    }
  }
}

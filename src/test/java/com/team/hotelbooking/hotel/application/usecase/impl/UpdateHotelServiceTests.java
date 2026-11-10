package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.command.UpdateHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.math.BigDecimal;
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
@DisplayName("Sửa thông tin khách sạn")
class UpdateHotelServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID OWNER_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID OTHER_USER_ID = UUID.fromString("2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d");
  private static final UUID MANAGER_ID = UUID.fromString("3b4c5d6e-7f8a-4b9c-8d1e-2f3a4b5c6d7e");
  private static final UUID ADMIN_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  private static final UUID CITY_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final Instant CREATED_AT = Instant.parse("2026-11-04T00:00:00Z");
  private static final Instant NOW = Instant.parse("2026-11-09T02:03:04Z");

  @Mock private HotelRepository hotels;

  private UpdateHotelService service;
  private Hotel hotel;

  @BeforeEach
  void setUp() {
    service = new UpdateHotelService(hotels, Clock.fixed(NOW, ZoneOffset.UTC));
    hotel =
        new Hotel(
            HOTEL_ID,
            OWNER_ID,
            CITY_ID,
            "Khách sạn Biển Xanh",
            "khach-san-bien-xanh",
            "Gần biển Mỹ Khê",
            "12 Võ Nguyên Giáp",
            null,
            null,
            new BigDecimal("16.0614"),
            new BigDecimal("108.2478"),
            (short) 4,
            LocalTime.of(14, 0),
            LocalTime.of(12, 0),
            Hotel.Status.ACTIVE,
            CREATED_AT,
            CREATED_AT,
            3L);
  }

  @Nested
  @DisplayName("UpdateHotelUseCase.execute - quyền")
  class AuthorizationTests {

    @Test
    void shouldUpdateAnyHotelWithoutMembershipCheckWhenActorIsAdmin() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      givenUpdateReturnsSavedHotel();

      // When
      ManagementHotelResult result = execute(command(ADMIN_ID, true).name("Tên mới"));

      // Then
      assertEquals("Tên mới", result.name());
      then(hotels).should(never()).findActiveMemberRoles(any(), any());
    }

    @Test
    void shouldUpdateOnlyProvidedFieldsWhenActorOwnsHotel() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      givenUpdateReturnsSavedHotel();

      // When
      ManagementHotelResult result =
          execute(
              command(OWNER_ID, false).name("  Khách sạn Biển Xanh Mới  ").starRating((short) 5));

      // Then
      ArgumentCaptor<Hotel> saved = ArgumentCaptor.forClass(Hotel.class);
      then(hotels).should().update(saved.capture(), eq(3L));
      Hotel updated = saved.getValue();
      assertEquals("Khách sạn Biển Xanh Mới", updated.name());
      assertEquals((short) 5, updated.starRating());
      assertEquals("Gần biển Mỹ Khê", updated.description());
      assertEquals("12 Võ Nguyên Giáp", updated.address());
      assertEquals(new BigDecimal("16.0614"), updated.latitude());
      assertEquals(LocalTime.of(14, 0), updated.checkInTime());
      assertEquals("khach-san-bien-xanh", updated.slug());
      assertEquals(OWNER_ID, updated.ownerId());
      assertEquals(Hotel.Status.ACTIVE, updated.status());
      assertEquals(CREATED_AT, updated.createdAt());
      assertEquals(NOW, updated.updatedAt());
      assertEquals("ACTIVE", result.status());
      then(hotels).should(never()).findActiveMemberRoles(any(), any());
    }

    @Test
    void shouldUpdateHotelWhenActorIsActiveManagerOfSameHotel() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(HOTEL_ID, MANAGER_ID))
          .willReturn(Set.of(HotelMemberRole.HOTEL_MANAGER));
      givenUpdateReturnsSavedHotel();

      // When
      ManagementHotelResult result = execute(command(MANAGER_ID, false).description("Mô tả mới"));

      // Then
      assertEquals("Mô tả mới", result.description());
    }

    @Test
    void shouldReturnNotFoundWhenActorIsNotMemberOfHotel() {
      // Given
      // Quản lý hotel khác, khách hàng hay Owner hotel khác đều không có vai trò ở hotel này.
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(HOTEL_ID, OTHER_USER_ID)).willReturn(Set.of());

      // When
      HotelException error =
          assertThrows(
              HotelException.class, () -> execute(command(OTHER_USER_ID, false).name("Tên mới")));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      then(hotels).should(never()).update(any(), anyLong());
    }

    @Test
    void shouldDenyAccessWhenActorIsReceptionistOfHotel() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(HOTEL_ID, OTHER_USER_ID))
          .willReturn(Set.of(HotelMemberRole.RECEPTIONIST));

      // When
      HotelException error =
          assertThrows(
              HotelException.class, () -> execute(command(OTHER_USER_ID, false).name("Tên mới")));

      // Then
      assertEquals(HotelErrorCode.ACCESS_DENIED, error.code());
      then(hotels).should(never()).update(any(), anyLong());
    }

    @Test
    void shouldUpdateHotelWhenActorHasBothReceptionistAndManagerRoles() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.findActiveMemberRoles(HOTEL_ID, MANAGER_ID))
          .willReturn(Set.of(HotelMemberRole.RECEPTIONIST, HotelMemberRole.HOTEL_MANAGER));
      givenUpdateReturnsSavedHotel();

      // When
      ManagementHotelResult result = execute(command(MANAGER_ID, false).name("Tên mới"));

      // Then
      assertEquals("Tên mới", result.name());
    }

    @Test
    void shouldReturnNotFoundWithoutMembershipCheckWhenNonAdminTargetsMissingHotel() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.empty());

      // When
      HotelException error =
          assertThrows(
              HotelException.class, () -> execute(command(OTHER_USER_ID, false).name("Tên mới")));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
      then(hotels).should(never()).findActiveMemberRoles(any(), any());
    }

    @Test
    void shouldReturnNotFoundWhenAdminTargetsMissingHotel() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.empty());

      // When
      HotelException error =
          assertThrows(HotelException.class, () -> execute(command(ADMIN_ID, true).name("Tên")));

      // Then
      assertEquals(HotelErrorCode.HOTEL_NOT_FOUND, error.code());
    }
  }

  @Nested
  @DisplayName("UpdateHotelUseCase.execute - dữ liệu")
  class ValidationTests {

    @Test
    void shouldPassClientVersionToRepositoryWhenVersionIsStale() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      given(hotels.update(any(Hotel.class), eq(2L)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // When
      execute(command(OWNER_ID, false).name("Tên mới").version(2L));

      // Then
      // Repository so version client gửi (2) với DB (3) và báo xung đột; service không tự đổi.
      then(hotels).should().update(any(Hotel.class), eq(2L));
    }

    @Test
    void shouldRejectRequestWhenVersionIsMissing() {
      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () -> execute(command(OWNER_ID, false).name("Tên").version(null)));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      then(hotels).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectRequestWhenNoFieldIsProvided() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

      // When
      HotelException error =
          assertThrows(HotelException.class, () -> execute(command(OWNER_ID, false)));

      // Then
      assertEquals(HotelErrorCode.INVALID_REQUEST, error.code());
      assertEquals("Phải có ít nhất một thông tin khách sạn cần cập nhật.", error.getMessage());
      then(hotels).should(never()).update(any(), anyLong());
    }

    @Test
    void shouldRejectRequestWhenNameIsBlank() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

      // When
      HotelException error =
          assertThrows(HotelException.class, () -> execute(command(OWNER_ID, false).name("   ")));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals("Tên khách sạn phải có từ 1 đến 255 ký tự.", error.getMessage());
    }

    @Test
    void shouldRejectRequestInsteadOfClearingWhenDescriptionIsBlank() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

      // When
      HotelException error =
          assertThrows(
              HotelException.class, () -> execute(command(OWNER_ID, false).description(" ")));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals(
          "Mô tả khách sạn không được để trống; chưa hỗ trợ xóa mô tả.", error.getMessage());
      then(hotels).should(never()).update(any(), anyLong());
    }

    @Test
    void shouldRejectRequestWhenOnlyLatitudeIsProvided() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () -> execute(command(OWNER_ID, false).coordinates(new BigDecimal("10.5"), null)));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals("Vĩ độ và kinh độ phải được gửi cùng nhau.", error.getMessage());
    }

    @Test
    void shouldRejectRequestWhenLongitudeIsOutOfRange() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

      // When
      HotelException error =
          assertThrows(
              HotelException.class,
              () ->
                  execute(
                      command(OWNER_ID, false)
                          .coordinates(new BigDecimal("10.5"), new BigDecimal("181"))));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals("Kinh độ phải nằm trong khoảng -180 đến 180.", error.getMessage());
    }

    @Test
    void shouldUpdateBothCoordinatesWhenPairIsProvided() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
      givenUpdateReturnsSavedHotel();

      // When
      ManagementHotelResult result =
          execute(
              command(OWNER_ID, false)
                  .coordinates(new BigDecimal("10.776"), new BigDecimal("106.7")));

      // Then
      assertEquals(new BigDecimal("10.776"), result.latitude());
      assertEquals(new BigDecimal("106.7"), result.longitude());
    }

    @Test
    void shouldRejectRequestWhenStarRatingIsOutOfRange() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

      // When
      HotelException error =
          assertThrows(
              HotelException.class, () -> execute(command(OWNER_ID, false).starRating((short) 6)));

      // Then
      assertEquals(HotelErrorCode.VALIDATION_FAILED, error.code());
      assertEquals("Số sao khách sạn phải từ 1 đến 5.", error.getMessage());
    }
  }

  private ManagementHotelResult execute(CommandBuilder builder) {
    return service.execute(builder.build());
  }

  private void givenUpdateReturnsSavedHotel() {
    given(hotels.update(any(Hotel.class), eq(3L)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  private static CommandBuilder command(UUID actorUserId, boolean admin) {
    return new CommandBuilder(actorUserId, admin);
  }

  // Builder nhỏ cho test: mỗi case chỉ ghi field liên quan, các field còn lại null.
  private static final class CommandBuilder {
    private final UUID actorUserId;
    private final boolean admin;
    private String name;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Short starRating;
    private Long version = 3L;

    private CommandBuilder(UUID actorUserId, boolean admin) {
      this.actorUserId = actorUserId;
      this.admin = admin;
    }

    CommandBuilder name(String value) {
      name = value;
      return this;
    }

    CommandBuilder description(String value) {
      description = value;
      return this;
    }

    CommandBuilder coordinates(BigDecimal lat, BigDecimal lng) {
      latitude = lat;
      longitude = lng;
      return this;
    }

    CommandBuilder starRating(Short value) {
      starRating = value;
      return this;
    }

    CommandBuilder version(Long value) {
      version = value;
      return this;
    }

    UpdateHotelCommand build() {
      return new UpdateHotelCommand(
          actorUserId,
          admin,
          HOTEL_ID,
          name,
          description,
          null,
          null,
          null,
          latitude,
          longitude,
          starRating,
          null,
          null,
          version);
    }
  }
}

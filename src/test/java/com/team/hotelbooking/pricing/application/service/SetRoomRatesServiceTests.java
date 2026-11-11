package com.team.hotelbooking.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelManagement;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelStatus;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.Level;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
@DisplayName("Dịch vụ thiết lập giá phòng theo ngày (PR02)")
class SetRoomRatesServiceTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");

  @Mock private PricingHotelPort pricingHotelPort;
  @Mock private RoomRateRepository roomRateRepository;

  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);

  private SetRoomRatesService service;

  private final UUID hotelId = UUID.randomUUID();
  private final UUID roomTypeId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new SetRoomRatesService(pricingHotelPort, roomRateRepository, fixedClock);
  }

  private void givenAccess(UUID hotelId, Level level, HotelStatus status) {
    given(pricingHotelPort.findManagementAccess(hotelId, ACTOR_ID, false))
        .willReturn(Optional.of(new HotelManagement(level, status)));
  }

  @Nested
  @DisplayName("SetRoomRatesUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Thiết lập giá thành công cho một khoảng ngày và ghi nhận người thực hiện (PR-08)")
    void shouldSetRatesSuccessfullyForDateRange() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 10);
      LocalDate endExclusive = LocalDate.of(2026, 11, 13); // 3 ngày: 10, 11, 12
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(
              ACTOR_ID, false, hotelId, roomTypeId, start, endExclusive, 1_500_000L, "VND");

      givenAccess(hotelId, Level.OWNER, HotelStatus.ACTIVE);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(true);
      given(
              roomRateRepository.findRatesByRoomTypeIdAndDateRange(
                  roomTypeId, start, LocalDate.of(2026, 11, 12)))
          .willReturn(Collections.emptyList());

      // When
      RoomTypeRatesResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.roomTypeId()).isEqualTo(roomTypeId);
      assertThat(result.startDate()).isEqualTo(start);
      assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 11, 12));
      assertThat(result.rates()).hasSize(3);
      assertThat(result.rates().get(0).price()).isEqualTo(1_500_000L);

      then(pricingHotelPort).should().findManagementAccess(hotelId, ACTOR_ID, false);
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<RoomRate>> captor = ArgumentCaptor.forClass(List.class);
      then(roomRateRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue())
          .hasSize(3)
          .allSatisfy(rate -> assertThat(rate.updatedByUserId()).isEqualTo(ACTOR_ID));
    }

    @Test
    @DisplayName("Thiết lập giá thành công cho 1 ngày khi endDateExclusive là null")
    void shouldSetRateSuccessfullyForSingleDayWhenEndDateNull() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 15);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(
              ACTOR_ID, false, hotelId, roomTypeId, start, null, 2_000_000L, "VND");

      givenAccess(hotelId, Level.OWNER, HotelStatus.ACTIVE);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(true);
      given(roomRateRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, start))
          .willReturn(Collections.emptyList());

      // When
      RoomTypeRatesResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.rates()).hasSize(1);
      assertThat(result.rates().get(0).date()).isEqualTo(start);
      assertThat(result.rates().get(0).price()).isEqualTo(2_000_000L);

      then(roomRateRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("Ném lỗi khi giá phòng nhỏ hơn hoặc bằng 0")
    void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 10);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(ACTOR_ID, false, hotelId, roomTypeId, start, null, 0L, "VND");

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.INVALID_AMOUNT));
    }

    @Test
    @DisplayName("Ném lỗi khi ngày bắt đầu trong quá khứ")
    void shouldThrowExceptionWhenStartDateInPast() {
      // Given (today is 2026-11-04)
      LocalDate pastDate = LocalDate.of(2026, 11, 1);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(
              ACTOR_ID, false, hotelId, roomTypeId, pastDate, null, 1_000_000L, "VND");

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    @DisplayName("Ném lỗi khi không tìm thấy khách sạn")
    void shouldThrowExceptionWhenHotelNotFound() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 10);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(
              ACTOR_ID, false, hotelId, roomTypeId, start, null, 1_000_000L, "VND");

      given(pricingHotelPort.findManagementAccess(hotelId, ACTOR_ID, false))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_NOT_FOUND));
    }

    @Test
    @DisplayName("Ném lỗi khi không tìm thấy loại phòng")
    void shouldThrowExceptionWhenRoomTypeNotFound() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 10);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(
              ACTOR_ID, false, hotelId, roomTypeId, start, null, 1_000_000L, "VND");

      givenAccess(hotelId, Level.OWNER, HotelStatus.ACTIVE);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(false);

      // When & Then
      assertThatThrownBy(() -> service.execute(command))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.ROOM_TYPE_NOT_FOUND));
    }
  }

  @Nested
  @DisplayName("Phân quyền theo khách sạn")
  class AccessTests {

    @Test
    @DisplayName("Ném ACCESS_DENIED và không xét khách sạn khi người gọi là RECEPTIONIST")
    void shouldThrowAccessDeniedWhenHotelReportsDenied() {
      // Given
      givenAccess(hotelId, Level.RECEPTIONIST, HotelStatus.ACTIVE);

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new SetRoomRatesCommand(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 10),
                          null,
                          1_000_000L,
                          "VND")))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.ACCESS_DENIED));
      then(pricingHotelPort).should(never()).isRoomTypeActiveInHotel(any(), any());
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName(
        "Ném HOTEL_NOT_FOUND khi người gọi không thuộc khách sạn hoặc khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelReportsNotFound() {
      // Given
      given(pricingHotelPort.findManagementAccess(hotelId, ACTOR_ID, false))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new SetRoomRatesCommand(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 10),
                          null,
                          1_000_000L,
                          "VND")))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_NOT_FOUND));
      then(pricingHotelPort).should(never()).isRoomTypeActiveInHotel(any(), any());
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_NOT_FOUND khi người gọi không có quan hệ nào với khách sạn")
    void shouldThrowHotelNotFoundWhenActorHasNoRelationToHotel() {
      // Given
      givenAccess(hotelId, Level.NONE, HotelStatus.ACTIVE);

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new SetRoomRatesCommand(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 10),
                          null,
                          1_000_000L,
                          "VND")))
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_NOT_FOUND));
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_LOCKED và không đụng giá khi khách sạn bị quản trị khóa, kể cả ADMIN")
    void shouldThrowHotelLockedWhenHotelIsLocked() {
      // Given
      givenAccess(hotelId, Level.ADMIN, HotelStatus.LOCKED);

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new SetRoomRatesCommand(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 10),
                          null,
                          1_000_000L,
                          "VND")))
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_LOCKED));
      then(pricingHotelPort).should(never()).isRoomTypeActiveInHotel(any(), any());
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Cho HOTEL_MANAGER thao tác giá khi khách sạn đang DRAFT (chưa/không mở bán)")
    void shouldAllowManagerWhenHotelIsDraft() {
      // Given
      givenAccess(hotelId, Level.MANAGER, HotelStatus.DRAFT);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(true);

      // When
      RoomTypeRatesResult result =
          service.execute(
              new SetRoomRatesCommand(
                  ACTOR_ID,
                  false,
                  hotelId,
                  roomTypeId,
                  LocalDate.of(2026, 11, 10),
                  null,
                  1_000_000L,
                  "VND"));

      // Then
      assertThat(result.hotelId()).isEqualTo(hotelId);
    }
  }
}

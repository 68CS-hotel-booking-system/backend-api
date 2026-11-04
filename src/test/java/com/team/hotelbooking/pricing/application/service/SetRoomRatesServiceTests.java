package com.team.hotelbooking.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.infrastructure.persistence.repository.RoomRateJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ thiết lập giá phòng theo ngày (PR02)")
class SetRoomRatesServiceTests {

  @Mock private PricingHotelPort pricingHotelPort;
  @Mock private RoomRateJpaRepository roomRateJpaRepository;

  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);

  private SetRoomRatesService service;

  private final UUID hotelId = UUID.randomUUID();
  private final UUID roomTypeId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new SetRoomRatesService(pricingHotelPort, roomRateJpaRepository, fixedClock);
  }

  @Nested
  @DisplayName("SetRoomRatesUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Thiết lập giá thành công cho một khoảng ngày")
    void shouldSetRatesSuccessfullyForDateRange() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 10);
      LocalDate endExclusive = LocalDate.of(2026, 11, 13); // 3 ngày: 10, 11, 12
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(hotelId, roomTypeId, start, endExclusive, 1_500_000L, "VND");

      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(true);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(true);
      given(
              roomRateJpaRepository.findRatesByRoomTypeIdAndDateRange(
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

      then(roomRateJpaRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("Thiết lập giá thành công cho 1 ngày khi endDateExclusive là null")
    void shouldSetRateSuccessfullyForSingleDayWhenEndDateNull() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 15);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(hotelId, roomTypeId, start, null, 2_000_000L, "VND");

      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(true);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(true);
      given(roomRateJpaRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, start))
          .willReturn(Collections.emptyList());

      // When
      RoomTypeRatesResult result = service.execute(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.rates()).hasSize(1);
      assertThat(result.rates().get(0).date()).isEqualTo(start);
      assertThat(result.rates().get(0).price()).isEqualTo(2_000_000L);

      then(roomRateJpaRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("Ném lỗi khi giá phòng nhỏ hơn hoặc bằng 0")
    void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
      // Given
      LocalDate start = LocalDate.of(2026, 11, 10);
      SetRoomRatesCommand command =
          new SetRoomRatesCommand(hotelId, roomTypeId, start, null, 0L, "VND");

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
          new SetRoomRatesCommand(hotelId, roomTypeId, pastDate, null, 1_000_000L, "VND");

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
          new SetRoomRatesCommand(hotelId, roomTypeId, start, null, 1_000_000L, "VND");

      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(false);

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
          new SetRoomRatesCommand(hotelId, roomTypeId, start, null, 1_000_000L, "VND");

      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(true);
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
}

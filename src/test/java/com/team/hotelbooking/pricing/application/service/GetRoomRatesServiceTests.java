package com.team.hotelbooking.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ tra cứu giá phòng theo ngày")
class GetRoomRatesServiceTests {

  @Mock private PricingHotelPort pricingHotelPort;
  @Mock private RoomRateRepository roomRateRepository;

  private GetRoomRatesService service;

  @BeforeEach
  void setUp() {
    service = new GetRoomRatesService(pricingHotelPort, roomRateRepository);
  }

  @Nested
  @DisplayName("GetRoomRatesUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Ném lỗi khi ngày kết thúc trước ngày bắt đầu hoặc vượt quá 90 ngày")
    void shouldThrowWhenDatesInvalid() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 10);
      LocalDate endBeforeStart = LocalDate.of(2026, 11, 5);
      LocalDate endTooFar = LocalDate.of(2027, 3, 15);

      // When & Then - Kết thúc trước bắt đầu
      PricingException dateOrderError =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new GetRoomRatesQuery(hotelId, roomTypeId, start, endBeforeStart)));
      assertEquals(PricingErrorCode.INVALID_DATE_RANGE, dateOrderError.code());

      // When & Then - Vượt quá 90 ngày
      PricingException maxRangeError =
          assertThrows(
              PricingException.class,
              () -> service.execute(new GetRoomRatesQuery(hotelId, roomTypeId, start, endTooFar)));
      assertEquals(PricingErrorCode.MAX_RANGE_EXCEEDED, maxRangeError.code());
    }

    @Test
    @DisplayName("Ném lỗi khi khách sạn hoặc loại phòng không tồn tại")
    void shouldThrowWhenHotelOrRoomTypeNotFound() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 1);
      LocalDate end = LocalDate.of(2026, 11, 5);

      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(false);

      // When & Then - Không thấy khách sạn
      PricingException hotelError =
          assertThrows(
              PricingException.class,
              () -> service.execute(new GetRoomRatesQuery(hotelId, roomTypeId, start, end)));
      assertEquals(PricingErrorCode.HOTEL_NOT_FOUND, hotelError.code());

      // Given - Khách sạn tồn tại nhưng loại phòng không thuộc khách sạn
      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(true);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(false);

      // When & Then - Không thấy loại phòng
      PricingException roomTypeError =
          assertThrows(
              PricingException.class,
              () -> service.execute(new GetRoomRatesQuery(hotelId, roomTypeId, start, end)));
      assertEquals(PricingErrorCode.ROOM_TYPE_NOT_FOUND, roomTypeError.code());
    }

    @Test
    @DisplayName("Trả về danh sách giá chính xác từng ngày khi tra cứu thành công")
    void shouldReturnRatesSuccessfully() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 1);
      LocalDate end = LocalDate.of(2026, 11, 2);
      Instant now = Instant.parse("2026-11-01T08:00:00Z");

      given(pricingHotelPort.isHotelActive(hotelId)).willReturn(true);
      given(pricingHotelPort.isRoomTypeActiveInHotel(roomTypeId, hotelId)).willReturn(true);

      RoomRate rate =
          new RoomRate(roomTypeId, start, 1_500_000L, "VND", UUID.randomUUID(), now, now);

      given(roomRateRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, end))
          .willReturn(List.of(rate));

      // When
      RoomTypeRatesResult result =
          service.execute(new GetRoomRatesQuery(hotelId, roomTypeId, start, end));

      // Then
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.roomTypeId()).isEqualTo(roomTypeId);
      assertThat(result.currency()).isEqualTo("VND");
      assertThat(result.rates()).hasSize(2);

      var day1 = result.rates().get(0);
      assertThat(day1.date()).isEqualTo(start);
      assertThat(day1.hasRate()).isTrue();
      assertThat(day1.price()).isEqualTo(1_500_000L);

      var day2 = result.rates().get(1);
      assertThat(day2.date()).isEqualTo(end);
      assertThat(day2.hasRate()).isFalse();
      assertThat(day2.price()).isNull();
    }
  }
}

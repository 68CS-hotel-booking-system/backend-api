package com.team.hotelbooking.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.application.result.CheckAvailabilityResult;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
@DisplayName("Dịch vụ tra cứu phòng trống")
class CheckAvailabilityServiceTests {

  @Mock private HotelCatalogPort hotelCatalogPort;
  @Mock private InventoryQueryPort inventoryQueryPort;
  @Mock private RoomPricingPort roomPricingPort;

  private Clock clock;
  private CheckAvailabilityService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);
    service =
        new CheckAvailabilityService(hotelCatalogPort, inventoryQueryPort, roomPricingPort, clock);
  }

  @Nested
  @DisplayName("CheckAvailabilityUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Ném lỗi khi ngày nhận phòng ở quá khứ hoặc ngày trả phòng trước ngày nhận")
    void shouldThrowWhenDatesInvalid() {
      // Given
      UUID hotelId = UUID.randomUUID();
      LocalDate today = LocalDate.now(clock);

      // When & Then - Check-in quá khứ
      InventoryException pastError =
          assertThrows(
              InventoryException.class,
              () ->
                  service.execute(
                      new CheckAvailabilityQuery(
                          hotelId, today.minusDays(1), today.plusDays(1), 2, 0, 1)));
      assertEquals(InventoryErrorCode.INVALID_DATE_RANGE, pastError.code());

      // When & Then - Check-out trước check-in
      InventoryException checkoutError =
          assertThrows(
              InventoryException.class,
              () ->
                  service.execute(
                      new CheckAvailabilityQuery(
                          hotelId, today.plusDays(5), today.plusDays(4), 2, 0, 1)));
      assertEquals(InventoryErrorCode.INVALID_DATE_RANGE, checkoutError.code());
    }

    @Test
    @DisplayName("Ném lỗi khi thời gian lưu trú vượt quá 30 đêm")
    void shouldThrowWhenStayExceeds30Nights() {
      // Given
      UUID hotelId = UUID.randomUUID();
      LocalDate today = LocalDate.now(clock);

      // When & Then
      InventoryException error =
          assertThrows(
              InventoryException.class,
              () ->
                  service.execute(
                      new CheckAvailabilityQuery(
                          hotelId, today.plusDays(1), today.plusDays(33), 2, 0, 1)));
      assertEquals(InventoryErrorCode.MAX_STAY_EXCEEDED, error.code());
    }

    @Test
    @DisplayName("Ném lỗi khi khách sạn không tồn tại hoặc tạm ngừng kinh doanh")
    void shouldThrowWhenHotelNotFoundOrSuspended() {
      // Given
      UUID hotelId = UUID.randomUUID();
      LocalDate today = LocalDate.now(clock);
      given(hotelCatalogPort.findHotelSummary(hotelId)).willReturn(Optional.empty());

      // When & Then
      InventoryException notFoundError =
          assertThrows(
              InventoryException.class,
              () ->
                  service.execute(
                      new CheckAvailabilityQuery(
                          hotelId, today.plusDays(1), today.plusDays(3), 2, 0, 1)));
      assertEquals(InventoryErrorCode.HOTEL_NOT_FOUND, notFoundError.code());

      // Given - Khách sạn bị tạm ngừng kinh doanh
      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, true)));

      // When & Then
      InventoryException suspendedError =
          assertThrows(
              InventoryException.class,
              () ->
                  service.execute(
                      new CheckAvailabilityQuery(
                          hotelId, today.plusDays(1), today.plusDays(3), 2, 0, 1)));
      assertEquals(InventoryErrorCode.HOTEL_NOT_FOUND, suspendedError.code());
    }

    @Test
    @DisplayName("Tính toán chính xác giá phòng kết hợp giá riêng và basePrice khi có phòng trống")
    void shouldCalculateAvailabilityAndPriceCorrectly() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate today = LocalDate.now(clock);
      LocalDate checkIn = today.plusDays(10);
      LocalDate checkOut = today.plusDays(12); // 2 đêm

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));

      HotelCatalogPort.RoomTypeSummary rt =
          new HotelCatalogPort.RoomTypeSummary(
              roomTypeId, "Deluxe Ocean View", 4, 2, 2, 1_000_000L);
      given(hotelCatalogPort.findActiveRoomTypes(hotelId)).willReturn(List.of(rt));

      given(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, checkIn, checkOut))
          .willReturn(2);

      given(roomPricingPort.getDailyRates(roomTypeId, checkIn, checkOut.minusDays(1)))
          .willReturn(Map.of(checkIn, 1_200_000L));

      // When
      CheckAvailabilityResult result =
          service.execute(new CheckAvailabilityQuery(hotelId, checkIn, checkOut, 2, 0, 1));

      // Then
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.totalNights()).isEqualTo(2);
      assertThat(result.roomTypes()).hasSize(1);

      var roomTypeRes = result.roomTypes().get(0);
      assertThat(roomTypeRes.availableQuantity()).isEqualTo(2);
      assertThat(roomTypeRes.isAvailable()).isTrue();
      assertThat(roomTypeRes.totalEstimatedPrice()).isEqualTo(2_200_000L); // 1.2M + 1.0M
      assertThat(roomTypeRes.pricePerNight()).isEqualTo(1_100_000L);
    }

    @Test
    @DisplayName(
        "Đánh dấu không khả dụng và không bán khi có đêm thiếu giá hợp lệ và basePrice bằng 0 (PR-01, PR-02, HT-03)")
    void shouldMarkRoomTypeUnavailableWhenAnyNightLacksValidPositiveRate() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate today = LocalDate.now(clock);
      LocalDate checkIn = today.plusDays(10);
      LocalDate checkOut = today.plusDays(12); // 2 đêm

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));

      HotelCatalogPort.RoomTypeSummary rt =
          new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Standard Room", 2, 2, 0, 0L);
      given(hotelCatalogPort.findActiveRoomTypes(hotelId)).willReturn(List.of(rt));

      given(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, checkIn, checkOut))
          .willReturn(3);

      // Chỉ có giá đêm đầu, đêm thứ hai rơi về basePrice = 0 -> không hợp lệ
      given(roomPricingPort.getDailyRates(roomTypeId, checkIn, checkOut.minusDays(1)))
          .willReturn(Map.of(checkIn, 800_000L));

      // When
      CheckAvailabilityResult result =
          service.execute(new CheckAvailabilityQuery(hotelId, checkIn, checkOut, 2, 0, 1));

      // Then
      assertThat(result.roomTypes()).hasSize(1);
      var roomTypeRes = result.roomTypes().get(0);
      assertThat(roomTypeRes.isAvailable()).isFalse();
      assertThat(roomTypeRes.availableQuantity()).isZero();
      assertThat(roomTypeRes.pricePerNight()).isNull();
      assertThat(roomTypeRes.totalEstimatedPrice()).isNull();
    }
  }
}

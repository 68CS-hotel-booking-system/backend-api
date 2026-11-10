package com.team.hotelbooking.pricing.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery.QuoteItemInput;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelPricingSnapshot;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.RoomTypePricingSnapshot;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
@DisplayName("Dịch vụ tính toán ngày/đêm, giờ nhận/trả, tiền phòng và phụ thu báo giá")
class CalculateStayQuoteServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final UUID DELUXE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  @Mock private PricingHotelPort pricingHotelPort;
  @Mock private RoomRateRepository roomRateRepository;

  private Clock clock;
  private CalculateStayQuoteService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"), ZoneOffset.UTC);
    service = new CalculateStayQuoteService(pricingHotelPort, roomRateRepository, clock);
  }

  @Nested
  @DisplayName("CalculateStayQuoteUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName(
        "Tính đúng số ngày/đêm, giờ nhận/trả mặc định và tiền phòng khi không phát sinh phụ thu")
    void shouldCalculateStandardQuoteWithoutSurchargesWhenUsingStandardHoursAndCapacity() {
      // Given
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 12);

      given(pricingHotelPort.findActiveHotel(HOTEL_ID))
          .willReturn(
              Optional.of(
                  new HotelPricingSnapshot(HOTEL_ID, LocalTime.of(14, 0), LocalTime.of(12, 0))));
      given(pricingHotelPort.findActiveRoomType(DELUXE_ID, HOTEL_ID))
          .willReturn(
              Optional.of(
                  new RoomTypePricingSnapshot(
                      DELUXE_ID, HOTEL_ID, "Deluxe King", 2, 1, 3, 1_000_000L)));
      given(
              roomRateRepository.findDailyAmounts(
                  DELUXE_ID, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11)))
          .willReturn(Map.of(LocalDate.of(2026, 11, 10), 800_000L));

      CalculateStayQuoteQuery query =
          new CalculateStayQuoteQuery(
              HOTEL_ID,
              checkIn,
              checkOut,
              null,
              null,
              List.of(new QuoteItemInput(DELUXE_ID, 2, 4, 0)));

      // When
      StayQuoteResult result = service.execute(query);

      // Then
      assertEquals(3, result.days());
      assertEquals(2, result.nights());
      assertEquals(LocalTime.of(14, 0), result.standardCheckInTime());
      assertEquals(LocalTime.of(12, 0), result.standardCheckOutTime());
      assertEquals(LocalTime.of(14, 0), result.expectedCheckInTime());
      assertEquals(LocalTime.of(12, 0), result.expectedCheckOutTime());
      assertEquals(3_600_000L, result.roomSubtotalAmount());
      assertEquals(0L, result.surchargeAmount());
      assertEquals(3_600_000L, result.subtotalAmount());
      assertEquals(0L, result.discountAmount());
      assertEquals(3_600_000L, result.totalAmount());
      assertEquals(1, result.items().size());
      assertEquals(2, result.items().get(0).nightlyPrices().size());
      assertEquals(800_000L, result.items().get(0).nightlyPrices().get(0).unitPrice());
      assertEquals(1_000_000L, result.items().get(0).nightlyPrices().get(1).unitPrice());
    }

    @Test
    @DisplayName("Tính đúng phụ thu nhận phòng sớm, trả phòng muộn và khách phụ vượt tiêu chuẩn")
    void shouldCalculateEarlyCheckInLateCheckOutAndExtraGuestSurcharges() {
      // Given
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 12);

      given(pricingHotelPort.findActiveHotel(HOTEL_ID))
          .willReturn(
              Optional.of(
                  new HotelPricingSnapshot(HOTEL_ID, LocalTime.of(14, 0), LocalTime.of(12, 0))));
      given(pricingHotelPort.findActiveRoomType(DELUXE_ID, HOTEL_ID))
          .willReturn(
              Optional.of(
                  new RoomTypePricingSnapshot(
                      DELUXE_ID, HOTEL_ID, "Deluxe King", 2, 1, 3, 1_000_000L)));
      given(
              roomRateRepository.findDailyAmounts(
                  DELUXE_ID, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11)))
          .willReturn(
              Map.of(
                  LocalDate.of(2026, 11, 10), 1_000_000L,
                  LocalDate.of(2026, 11, 11), 1_000_000L));

      // 1 phòng: 2 người lớn + 1 trẻ em (tổng 3 khách > maxAdults 2 => 1 khách phụ)
      // Nhận sớm 08:00 (khung 05:00-09:00 => 50% đêm đầu = 500_000)
      // Trả muộn 14:00 (khung 12:00-15:00 => 30% đêm cuối = 300_000)
      // Phụ thu khách phụ: 1 khách × 10% giá mỗi đêm = 100_000 + 100_000 = 200_000
      CalculateStayQuoteQuery query =
          new CalculateStayQuoteQuery(
              HOTEL_ID,
              checkIn,
              checkOut,
              LocalTime.of(8, 0),
              LocalTime.of(14, 0),
              List.of(new QuoteItemInput(DELUXE_ID, 1, 2, 1)));

      // When
      StayQuoteResult result = service.execute(query);

      // Then
      assertEquals(2_000_000L, result.roomSubtotalAmount());
      assertEquals(500_000L, result.earlyCheckInSurchargeAmount());
      assertEquals(300_000L, result.lateCheckOutSurchargeAmount());
      assertEquals(200_000L, result.extraGuestSurchargeAmount());
      assertEquals(1_000_000L, result.surchargeAmount());
      assertEquals(3_000_000L, result.subtotalAmount());
      assertEquals(3_000_000L, result.totalAmount());
    }

    @Test
    @DisplayName("Từ chối báo giá khi số lượng khách vượt quá sức chứa tối đa của loại phòng")
    void shouldThrowInvalidOccupancyWhenGuestsExceedMaxOccupancy() {
      // Given
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 11);

      given(pricingHotelPort.findActiveHotel(HOTEL_ID))
          .willReturn(
              Optional.of(
                  new HotelPricingSnapshot(HOTEL_ID, LocalTime.of(14, 0), LocalTime.of(12, 0))));
      given(pricingHotelPort.findActiveRoomType(DELUXE_ID, HOTEL_ID))
          .willReturn(
              Optional.of(
                  new RoomTypePricingSnapshot(
                      DELUXE_ID, HOTEL_ID, "Deluxe King", 2, 1, 3, 1_000_000L)));

      CalculateStayQuoteQuery query =
          new CalculateStayQuoteQuery(
              HOTEL_ID,
              checkIn,
              checkOut,
              null,
              null,
              List.of(new QuoteItemInput(DELUXE_ID, 1, 2, 2)));

      // When
      PricingException error = assertThrows(PricingException.class, () -> service.execute(query));

      // Then
      assertEquals(PricingErrorCode.INVALID_OCCUPANCY, error.code());
    }

    @Test
    @DisplayName("Từ chối báo giá khi có đêm lưu trú chưa có giá bán hợp lệ")
    void shouldThrowRateNotAvailableWhenNightHasZeroPrice() {
      // Given
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 12);

      given(pricingHotelPort.findActiveHotel(HOTEL_ID))
          .willReturn(
              Optional.of(
                  new HotelPricingSnapshot(HOTEL_ID, LocalTime.of(14, 0), LocalTime.of(12, 0))));
      given(pricingHotelPort.findActiveRoomType(DELUXE_ID, HOTEL_ID))
          .willReturn(
              Optional.of(
                  new RoomTypePricingSnapshot(DELUXE_ID, HOTEL_ID, "Deluxe King", 2, 0, 2, 0L)));
      given(
              roomRateRepository.findDailyAmounts(
                  DELUXE_ID, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11)))
          .willReturn(Map.of(LocalDate.of(2026, 11, 10), 900_000L));

      CalculateStayQuoteQuery query =
          new CalculateStayQuoteQuery(
              HOTEL_ID,
              checkIn,
              checkOut,
              null,
              null,
              List.of(new QuoteItemInput(DELUXE_ID, 1, 2, 0)));

      // When
      PricingException error = assertThrows(PricingException.class, () -> service.execute(query));

      // Then
      assertEquals(PricingErrorCode.RATE_NOT_AVAILABLE, error.code());
    }
  }
}

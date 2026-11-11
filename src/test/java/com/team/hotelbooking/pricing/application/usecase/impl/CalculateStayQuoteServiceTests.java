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
@DisplayName("Dịch vụ tính toán ngày/đêm, giờ nhận/trả, tiền phòng theo đêm")
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
      "0, 1, 0",
      "2, 1, 0",
      "1, 1, -1",
      "1, 3, 0",
      "1, 1, 3",
      "2, 4, 3"
    })
    @DisplayName("Từ chối số phòng, số khách và tổng sức chứa không hợp lệ")
    void shouldRejectInvalidGuestDistribution(int quantity, int adults, int children) {
      given(pricingHotelPort.findActiveHotel(HOTEL_ID))
          .willReturn(
              Optional.of(
                  new HotelPricingSnapshot(HOTEL_ID, LocalTime.of(14, 0), LocalTime.of(12, 0))));
      given(pricingHotelPort.findActiveRoomType(DELUXE_ID, HOTEL_ID))
          .willReturn(
              Optional.of(
                  new RoomTypePricingSnapshot(DELUXE_ID, HOTEL_ID, "Deluxe", 2, 2, 3, 1_000_000L)));

      PricingException error =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new CalculateStayQuoteQuery(
                          HOTEL_ID,
                          LocalDate.of(2026, 11, 10),
                          LocalDate.of(2026, 11, 12),
                          null,
                          null,
                          List.of(new QuoteItemInput(DELUXE_ID, quantity, adults, children)))));

      assertEquals(PricingErrorCode.INVALID_OCCUPANCY, error.code());
    }

    @Test
    @DisplayName("Sau nửa đêm Việt Nam phải từ chối ngày hôm qua dù UTC chưa sang ngày")
    void shouldValidateCheckInAgainstVietnamDateAtUtcDayBoundary() {
      service =
          new CalculateStayQuoteService(
              pricingHotelPort,
              roomRateRepository,
              Clock.fixed(Instant.parse("2026-11-09T18:00:00Z"), ZoneOffset.UTC));
      PricingException error =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new CalculateStayQuoteQuery(
                          HOTEL_ID,
                          LocalDate.of(2026, 11, 9),
                          LocalDate.of(2026, 11, 11),
                          null,
                          null,
                          List.of(new QuoteItemInput(DELUXE_ID, 1, 1, 0)))));
      assertEquals(PricingErrorCode.INVALID_DATE_RANGE, error.code());
    }

    @Test
    @DisplayName("Cộng nhiều hạng mục theo số phòng nhân tổng giá đêm, trẻ em không làm tăng giá")
    void shouldSumMultipleItemsWithinCombinedCapacityWithoutSurcharges() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      given(pricingHotelPort.findActiveHotel(HOTEL_ID))
          .willReturn(
              Optional.of(
                  new HotelPricingSnapshot(HOTEL_ID, LocalTime.of(15, 0), LocalTime.of(11, 0))));
      given(pricingHotelPort.findActiveRoomType(DELUXE_ID, HOTEL_ID))
          .willReturn(
              Optional.of(
                  new RoomTypePricingSnapshot(DELUXE_ID, HOTEL_ID, "Deluxe", 2, 2, 3, 1_000_000L)));
      given(roomRateRepository.findDailyAmounts(DELUXE_ID, checkIn, checkIn.plusDays(1)))
          .willReturn(Map.of(checkIn, 800_000L));

      StayQuoteResult result =
          service.execute(
              new CalculateStayQuoteQuery(
                  HOTEL_ID,
                  checkIn,
                  checkIn.plusDays(2),
                  LocalTime.of(4, 0),
                  LocalTime.of(23, 0),
                  List.of(
                      new QuoteItemInput(DELUXE_ID, 2, 3, 3),
                      new QuoteItemInput(DELUXE_ID, 1, 1, 0))));

      assertEquals(5_400_000L, result.totalAmount());
      assertEquals(result.subtotalAmount(), result.totalAmount());
      assertEquals(0L, result.discountAmount());
      assertEquals(0L, result.surchargeAmount());
      assertEquals(3_600_000L, result.items().get(0).subtotalAmount());
      assertEquals(1_800_000L, result.items().get(1).subtotalAmount());
      assertEquals(LocalTime.of(15, 0), result.standardCheckInTime());
      assertEquals(LocalTime.of(11, 0), result.standardCheckOutTime());
    }

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
    @DisplayName("Giờ dự kiến và trẻ em trong sức chứa không làm tăng tiền phòng")
    void shouldKeepRoomPriceWhenExpectedHoursAndGuestCountsChange() {
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
      assertEquals(0L, result.earlyCheckInSurchargeAmount());
      assertEquals(0L, result.lateCheckOutSurchargeAmount());
      assertEquals(0L, result.extraGuestSurchargeAmount());
      assertEquals(0L, result.surchargeAmount());
      assertEquals(2_000_000L, result.subtotalAmount());
      assertEquals(2_000_000L, result.totalAmount());
      assertEquals(LocalTime.of(8, 0), result.expectedCheckInTime());
      assertEquals(LocalTime.of(14, 0), result.expectedCheckOutTime());
      assertEquals(0, result.items().get(0).extraGuests());
      assertEquals(0L, result.items().get(0).surchargeAmount());
      assertEquals(2_000_000L, result.items().get(0).subtotalAmount());
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
                      DELUXE_ID, HOTEL_ID, "Deluxe King", 2, 2, 3, 1_000_000L)));

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
                  new RoomTypePricingSnapshot(
                      DELUXE_ID, HOTEL_ID, "Deluxe King", 2, 0, 2, 1_000_000L)));
      given(
              roomRateRepository.findDailyAmounts(
                  DELUXE_ID, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11)))
          .willReturn(Map.of(LocalDate.of(2026, 11, 10), 0L));

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

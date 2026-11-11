package com.team.hotelbooking.pricing.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.pricing.application.command.ListNightlyRatesQuery;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.RoomTypePricingSnapshot;
import com.team.hotelbooking.pricing.application.result.RoomTypeNightlyRatesResult;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tra giá từng đêm theo lô cho tìm kiếm")
class ListNightlyRatesServiceTests {

  private static final UUID HOTEL_ID = UUID.fromString("1a000000-0000-4000-8000-000000000001");
  private static final UUID PRICED_ID = UUID.fromString("1b000000-0000-4000-8000-000000000001");
  private static final UUID GAP_ID = UUID.fromString("1b000000-0000-4000-8000-000000000002");
  private static final UUID INACTIVE_ID = UUID.fromString("1b000000-0000-4000-8000-000000000003");
  private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
  private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 3);

  @Mock private PricingHotelPort pricingHotelPort;
  @Mock private RoomRateRepository roomRateRepository;

  private ListNightlyRatesService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-11-10T00:00:00Z"), ZoneOffset.UTC);
    service = new ListNightlyRatesService(pricingHotelPort, roomRateRepository, clock);
  }

  private static RoomTypePricingSnapshot roomType(UUID id, long basePrice) {
    return new RoomTypePricingSnapshot(id, HOTEL_ID, "Phòng", 2, 0, 2, basePrice);
  }

  @Nested
  @DisplayName("ListNightlyRatesUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Một lượt đọc loại phòng và một lượt đọc room_rates cho cả lô")
    void shouldResolveAllRoomTypesWithOneRateQueryWhenBatchRequested() {
      // Given
      given(pricingHotelPort.findActiveRoomTypes(Set.of(PRICED_ID, GAP_ID, INACTIVE_ID)))
          .willReturn(List.of(roomType(PRICED_ID, 800_000L), roomType(GAP_ID, 0L)));
      given(
              roomRateRepository.findDailyAmounts(
                  List.of(PRICED_ID, GAP_ID), CHECK_IN, CHECK_OUT.minusDays(1)))
          .willReturn(
              Map.of(PRICED_ID, Map.of(CHECK_IN, 1_000_000L), GAP_ID, Map.of(CHECK_IN, 1L)));

      // When
      List<RoomTypeNightlyRatesResult> results =
          service.execute(
              new ListNightlyRatesQuery(
                  Set.of(PRICED_ID, GAP_ID, INACTIVE_ID), CHECK_IN, CHECK_OUT));

      // Then: loại phòng không đang bán bị bỏ; GAP thiếu giá đêm thứ hai
      assertThat(results)
          .extracting(RoomTypeNightlyRatesResult::roomTypeId)
          .containsExactlyInAnyOrder(PRICED_ID, GAP_ID);
      RoomTypeNightlyRatesResult priced =
          results.stream().filter(r -> r.roomTypeId().equals(PRICED_ID)).findFirst().orElseThrow();
      assertThat(priced.sellable()).isTrue();
      assertThat(priced.totalAmount()).isEqualTo(1_800_000L);
      assertThat(priced.hotelId()).isEqualTo(HOTEL_ID);
      assertThat(
              results.stream()
                  .filter(r -> r.roomTypeId().equals(GAP_ID))
                  .findFirst()
                  .orElseThrow()
                  .sellable())
          .isFalse();
    }

    @Test
    @DisplayName("Không đọc gì khi danh sách loại phòng rỗng")
    void shouldReturnEmptyWithoutQueriesWhenNoRoomTypes() {
      assertThat(service.execute(new ListNightlyRatesQuery(Set.of(), CHECK_IN, CHECK_OUT)))
          .isEmpty();
      then(pricingHotelPort).shouldHaveNoInteractions();
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Kiểm ngày giống báo giá: quá 30 đêm trả MAX_STAY_EXCEEDED")
    void shouldRejectLikeQuoteWhenStayExceedsThirtyNights() {
      assertThatThrownBy(
              () ->
                  service.execute(
                      new ListNightlyRatesQuery(
                          Set.of(PRICED_ID), CHECK_IN, CHECK_IN.plusDays(31))))
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.MAX_STAY_EXCEEDED));
      then(pricingHotelPort).shouldHaveNoInteractions();
    }
  }
}

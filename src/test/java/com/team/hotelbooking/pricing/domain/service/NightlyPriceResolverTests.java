package com.team.hotelbooking.pricing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.StayQuote.NightlyPrice;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Quy tắc giá từng đêm dùng chung cho báo giá và tìm kiếm (PR-02)")
class NightlyPriceResolverTests {

  private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
  private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 4);

  @Nested
  @DisplayName("NightlyPriceResolver.resolve")
  class ResolveTests {

    @Test
    @DisplayName("Ưu tiên room_rates, đêm không có giá riêng dùng base_price")
    void shouldPreferDailyRateAndFallBackToBasePrice() {
      // When
      NightlyPriceResolver.Resolution resolution =
          NightlyPriceResolver.resolve(
              1_000_000L, Map.of(CHECK_IN.plusDays(1), 1_500_000L), CHECK_IN, CHECK_OUT);

      // Then
      assertThat(resolution.sellable()).isTrue();
      assertThat(resolution.nightlyPrices())
          .containsExactly(
              new NightlyPrice(CHECK_IN, 1_000_000L),
              new NightlyPrice(CHECK_IN.plusDays(1), 1_500_000L),
              new NightlyPrice(CHECK_IN.plusDays(2), 1_000_000L));
      assertThat(resolution.totalAmount()).isEqualTo(3_500_000L);
    }

    @Test
    @DisplayName("Không bán được khi một đêm không có giá riêng và base_price = 0")
    void shouldBeUnsellableWhenNightFallsBackToZeroBasePrice() {
      // When
      NightlyPriceResolver.Resolution resolution =
          NightlyPriceResolver.resolve(
              0L, Map.of(CHECK_IN, 900_000L, CHECK_IN.plusDays(2), 900_000L), CHECK_IN, CHECK_OUT);

      // Then
      assertThat(resolution.sellable()).isFalse();
      assertThat(resolution.firstUnpricedNight()).isEqualTo(CHECK_IN.plusDays(1));
    }

    @Test
    @DisplayName("Giá riêng bằng 0 cũng là chưa có giá hợp lệ, dù base_price > 0")
    void shouldBeUnsellableWhenDailyRateIsZero() {
      assertThat(
              NightlyPriceResolver.resolve(1_000_000L, Map.of(CHECK_IN, 0L), CHECK_IN, CHECK_OUT)
                  .sellable())
          .isFalse();
    }
  }

  @Nested
  @DisplayName("NightlyPriceResolver.validateStay")
  class ValidateStayTests {

    @Test
    @DisplayName("Từ chối quá 30 đêm")
    void shouldRejectWhenStayExceedsThirtyNights() {
      assertThatThrownBy(
              () -> NightlyPriceResolver.validateStay(CHECK_IN, CHECK_IN.plusDays(31), CHECK_IN))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.MAX_STAY_EXCEEDED));
    }

    @Test
    @DisplayName("Từ chối ngày nhận phòng trong quá khứ")
    void shouldRejectWhenCheckInIsInPast() {
      assertThatThrownBy(
              () -> NightlyPriceResolver.validateStay(CHECK_IN, CHECK_OUT, CHECK_IN.plusDays(1)))
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.INVALID_DATE_RANGE));
    }
  }
}

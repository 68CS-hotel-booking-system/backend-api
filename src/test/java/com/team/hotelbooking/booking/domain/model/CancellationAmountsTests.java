package com.team.hotelbooking.booking.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CancellationAmountsTests {

  @ParameterizedTest
  @CsvSource({
    "2026-11-09T06:59:59Z, false, 0, 101, 0",
    "2026-11-09T07:00:00Z, false, 0, 101, 0",
    "2026-11-09T07:00:01Z, false, 101, 0, 13",
    "2026-11-09T07:00:01Z, true, 0, 101, 0"
  })
  @DisplayName("Tính phí, tiền hoàn và hoa hồng tại mốc 24 giờ theo giờ Việt Nam")
  void shouldApplyCancellationTerms(
      String time, boolean cancelledByHotel, long fee, long refund, long commission) {
    // Given
    Booking booking = mock(Booking.class);
    given(booking.checkIn()).willReturn(LocalDate.of(2026, 11, 10));
    given(booking.standardCheckInTime()).willReturn(LocalTime.of(14, 0));

    // When
    var amounts =
        CancellationAmounts.calculate(
            booking, Instant.parse(time), cancelledByHotel, 101, new BigDecimal("12.50"));

    // Then
    assertThat(amounts).isEqualTo(new CancellationAmounts(fee, refund, commission));
  }
}

package com.team.hotelbooking.inventory.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.hotelbooking.inventory.domain.model.RoomBlockSpan;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Tính tồn nhỏ nhất qua các đêm (IV-03)")
class NightlyAvailabilityCalculatorTests {

  private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
  private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 4);
  private static final UUID ROOM_A = UUID.fromString("0f000000-0000-4000-8000-00000000000a");
  private static final UUID ROOM_B = UUID.fromString("0f000000-0000-4000-8000-00000000000b");

  @Nested
  @DisplayName("NightlyAvailabilityCalculator.minAvailable")
  class MinAvailableTests {

    @Test
    @DisplayName("Trả 0 khi loại phòng không có phòng đang phục vụ")
    void shouldReturnZeroWhenNoOperationalRooms() {
      assertThat(
              NightlyAvailabilityCalculator.minAvailable(
                  0, List.of(), Map.of(), CHECK_IN, CHECK_OUT))
          .isZero();
    }

    @Test
    @DisplayName("Lấy đêm thấp nhất, trừ cả phòng chặn, chặn chưa gán phòng và phòng đã giữ")
    void shouldReturnLowestNightWhenBlocksAndReservationsDiffer() {
      // Given: đêm 1 trừ 1 giữ; đêm 2 trừ 1 phòng chặn + 1 chặn chưa gán + 1 giữ; đêm 3 trống
      List<RoomBlockSpan> blocks =
          List.of(
              new RoomBlockSpan(ROOM_A, CHECK_IN.plusDays(1), CHECK_IN.plusDays(1)),
              new RoomBlockSpan(null, CHECK_IN.plusDays(1), CHECK_IN.plusDays(1)));
      Map<LocalDate, Long> occupied = Map.of(CHECK_IN, 1L, CHECK_IN.plusDays(1), 1L);

      // When
      int available =
          NightlyAvailabilityCalculator.minAvailable(5, blocks, occupied, CHECK_IN, CHECK_OUT);

      // Then
      assertThat(available).isEqualTo(2);
    }

    @Test
    @DisplayName("Không đếm trùng một phòng vật lý có nhiều lịch chặn chồng nhau")
    void shouldCountPhysicalRoomOnceWhenBlocksOverlap() {
      // Given
      List<RoomBlockSpan> blocks =
          List.of(
              new RoomBlockSpan(ROOM_A, CHECK_IN, CHECK_OUT),
              new RoomBlockSpan(ROOM_A, CHECK_IN, CHECK_IN.plusDays(1)),
              new RoomBlockSpan(ROOM_B, CHECK_IN.plusDays(2), CHECK_IN.plusDays(2)));

      // When
      int available =
          NightlyAvailabilityCalculator.minAvailable(3, blocks, Map.of(), CHECK_IN, CHECK_OUT);

      // Then: đêm 1–2 trừ 1 (ROOM_A), đêm 3 trừ 2 (ROOM_A + ROOM_B)
      assertThat(available).isEqualTo(1);
    }

    @Test
    @DisplayName("Không trả số âm khi giữ phòng vượt tồn")
    void shouldNotReturnNegativeWhenOverbooked() {
      assertThat(
              NightlyAvailabilityCalculator.minAvailable(
                  2, List.of(), Map.of(CHECK_IN, 5L), CHECK_IN, CHECK_OUT))
          .isZero();
    }

    @Test
    @DisplayName("Ngày trả phòng không tính là một đêm")
    void shouldIgnoreCheckOutDateWhenBlockStartsThatDay() {
      List<RoomBlockSpan> blocks = List.of(new RoomBlockSpan(null, CHECK_OUT, CHECK_OUT));

      assertThat(
              NightlyAvailabilityCalculator.minAvailable(1, blocks, Map.of(), CHECK_IN, CHECK_OUT))
          .isEqualTo(1);
    }
  }
}

package com.team.hotelbooking.inventory.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.PhysicalRoom;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.StayDemand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Bộ kiểm tra phân bổ phòng vật lý liên tục (IV-13)")
class RoomAllocationPlannerTests {

  private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
  private static final LocalDate D10 = LocalDate.of(2026, 11, 10);
  private static final LocalDate D11 = LocalDate.of(2026, 11, 11);
  private static final LocalDate D12 = LocalDate.of(2026, 11, 12);
  private static final LocalDate D13 = LocalDate.of(2026, 11, 13);

  @Nested
  @DisplayName("RoomAllocationPlanner.calculateMaxAllocatable")
  class CalculateMaxAllocatableTests {

    @Test
    @DisplayName(
        "Trả về 0 khi phòng 301 bảo trì đêm 10/11 và phòng 302 bảo trì đêm 11/11 dù mỗi đêm còn 1 phòng (IV-13)")
    void shouldReturnZeroWhenNoSingleRoomIsContinuouslyFreeAcrossStay() {
      // Given
      PhysicalRoom room301 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of(D10));
      PhysicalRoom room302 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of(D11));

      // When
      int available =
          RoomAllocationPlanner.calculateMaxAllocatable(
              List.of(room301, room302), List.of(), D10, D12, NOW);

      // Then
      assertEquals(0, available);
    }

    @Test
    @DisplayName(
        "Trả về 1 khi hai đơn 1 đêm liên tiếp có thể ghép chung vào một phòng, nhường phòng còn lại cho kỳ 2 đêm")
    void shouldReturnOneWhenSequentialSingleNightDemandsCanShareOneRoom() {
      // Given
      PhysicalRoom room301 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of());
      PhysicalRoom room302 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of());
      List<StayDemand> existingDemands =
          List.of(new StayDemand(D10, D11, 1), new StayDemand(D11, D12, 1));

      // When
      int available =
          RoomAllocationPlanner.calculateMaxAllocatable(
              List.of(room301, room302), existingDemands, D10, D12, NOW);

      // Then
      assertEquals(1, available);
    }

    @Test
    @DisplayName("Trả về 0 khi đơn hiện hữu bị ép dùng phòng duy nhất không bị chặn ở đêm kế tiếp")
    void shouldReturnZeroWhenExistingDemandForcesConflictOnOnlyUnblockedRoom() {
      // Given: 301 bị chặn đêm 10/11 và 12/11; 302 trống hoàn toàn.
      // Đơn hiện hữu ở [11/11, 13/11) buộc phải dùng 302, nên đơn mới [10/11, 12/11) không thể dùng
      // 302 hay 301.
      PhysicalRoom room301 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of(D10, D12));
      PhysicalRoom room302 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of());
      List<StayDemand> existingDemands = List.of(new StayDemand(D11, D13, 1));

      // When
      int available =
          RoomAllocationPlanner.calculateMaxAllocatable(
              List.of(room301, room302), existingDemands, D10, D12, NOW);

      // Then
      assertEquals(0, available);
    }

    @Test
    @DisplayName(
        "Loại trừ phòng đã ngừng phục vụ theo operationalUntil khi tính khả dụng nhiều đêm (HT-12)")
    void shouldExcludeRoomWhenDeactivatedBeforeSecondNight() {
      // Given: 301 ngừng phục vụ từ 2026-11-11T00:00:00Z, 302 đã ngừng từ trước NOW
      PhysicalRoom room301 =
          new PhysicalRoom(
              UUID.randomUUID(), null, Instant.parse("2026-11-11T00:00:00Z"), Set.of());
      PhysicalRoom room302 =
          new PhysicalRoom(
              UUID.randomUUID(), null, Instant.parse("2026-11-10T08:00:00Z"), Set.of());

      // When
      int availableSingleNight =
          RoomAllocationPlanner.calculateMaxAllocatable(
              List.of(room301, room302), List.of(), D10, D11, NOW);
      int availableTwoNights =
          RoomAllocationPlanner.calculateMaxAllocatable(
              List.of(room301, room302), List.of(), D10, D12, NOW);

      // Then
      assertEquals(1, availableSingleNight);
      assertEquals(0, availableTwoNights);
    }
  }

  @Nested
  @DisplayName("RoomAllocationPlanner.canAllocate")
  class CanAllocateTests {

    @Test
    @DisplayName("Kiểm tra phân bổ thành công và thất bại chính xác cho tập nhu cầu")
    void shouldValidateAllocationCorrectly() {
      // Given
      PhysicalRoom room301 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of(D10));
      PhysicalRoom room302 = new PhysicalRoom(UUID.randomUUID(), null, null, Set.of());

      // When & Then
      assertTrue(
          RoomAllocationPlanner.canAllocate(
              List.of(room301, room302),
              List.of(new StayDemand(D10, D12, 1), new StayDemand(D11, D12, 1)),
              NOW));
      assertFalse(
          RoomAllocationPlanner.canAllocate(
              List.of(room301, room302), List.of(new StayDemand(D10, D12, 2)), NOW));
    }
  }
}

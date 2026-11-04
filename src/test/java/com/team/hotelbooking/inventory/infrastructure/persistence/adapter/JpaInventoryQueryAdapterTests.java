package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.DailyReservationOccupancy;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
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
@DisplayName("Adapter truy vấn khả dụng phòng vật lý và giữ chỗ")
class JpaInventoryQueryAdapterTests {

  @Mock private RoomJpaRepository roomJpaRepository;
  @Mock private RoomBlockJpaRepository roomBlockJpaRepository;
  @Mock private InventoryReservationJpaRepository reservationJpaRepository;

  private JpaInventoryQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter =
        new JpaInventoryQueryAdapter(
            roomJpaRepository, roomBlockJpaRepository, reservationJpaRepository);
  }

  @Nested
  @DisplayName("calculateAvailableQuantity")
  class CalculateAvailableQuantityTests {

    @Test
    @DisplayName("Trả về 0 khi loại phòng không có phòng vật lý nào hoạt động")
    void shouldReturnZeroWhenNoActiveRooms() {
      // Given
      UUID roomTypeId = UUID.randomUUID();
      given(roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId)).willReturn(0L);

      // When
      int available =
          adapter.calculateAvailableQuantity(
              roomTypeId, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

      // Then
      assertThat(available).isZero();
    }

    @Test
    @DisplayName(
        "Tính toán chính xác giá trị nhỏ nhất theo dải ngày và chống tính trùng phòng bị chặn")
    void shouldCalculateMinAvailableWithoutDuplicateBlocks() {
      // Given
      UUID roomTypeId = UUID.randomUUID();
      UUID physicalRoom1 = UUID.randomUUID();
      UUID physicalRoom2 = UUID.randomUUID();

      given(roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId)).willReturn(10L);

      // Block 1: physicalRoom1 bị chặn từ 10/11 đến 11/11
      RoomBlockJpaEntity block1 = new RoomBlockJpaEntity();
      block1.setRoomId(physicalRoom1);
      block1.setStartDate(LocalDate.of(2026, 11, 10));
      block1.setEndDate(LocalDate.of(2026, 11, 11));

      // Block 2: Cùng physicalRoom1 bị trùng lịch chặn khác ngày 10/11 -> không tính trùng
      RoomBlockJpaEntity block2 = new RoomBlockJpaEntity();
      block2.setRoomId(physicalRoom1);
      block2.setStartDate(LocalDate.of(2026, 11, 10));
      block2.setEndDate(LocalDate.of(2026, 11, 10));

      // Block 3: physicalRoom2 bị chặn ngày 11/11
      RoomBlockJpaEntity block3 = new RoomBlockJpaEntity();
      block3.setRoomId(physicalRoom2);
      block3.setStartDate(LocalDate.of(2026, 11, 11));
      block3.setEndDate(LocalDate.of(2026, 11, 11));

      given(roomBlockJpaRepository.findActiveBlocksInRange(eq(roomTypeId), any(), any()))
          .willReturn(List.of(block1, block2, block3));

      DailyReservationOccupancy occ1 = mock(DailyReservationOccupancy.class);
      given(occ1.getStayDate()).willReturn(LocalDate.of(2026, 11, 10));
      given(occ1.getTotalQuantity()).willReturn(3L);

      DailyReservationOccupancy occ2 = mock(DailyReservationOccupancy.class);
      given(occ2.getStayDate()).willReturn(LocalDate.of(2026, 11, 11));
      given(occ2.getTotalQuantity()).willReturn(5L);

      given(reservationJpaRepository.sumOccupancyByDateRange(eq(roomTypeId), any(), any(), any()))
          .willReturn(List.of(occ1, occ2));

      // When
      int available =
          adapter.calculateAvailableQuantity(
              roomTypeId, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

      // Then
      assertThat(available).isEqualTo(3);
    }
  }
}

package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;

class JpaInventoryQueryAdapterTest {

  private RoomJpaRepository roomJpaRepository;
  private RoomBlockJpaRepository roomBlockJpaRepository;
  private InventoryReservationJpaRepository reservationJpaRepository;
  private JpaInventoryQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    roomJpaRepository = mock(RoomJpaRepository.class);
    roomBlockJpaRepository = mock(RoomBlockJpaRepository.class);
    reservationJpaRepository = mock(InventoryReservationJpaRepository.class);
    adapter =
        new JpaInventoryQueryAdapter(
            roomJpaRepository, roomBlockJpaRepository, reservationJpaRepository);
  }

  @Test
  @DisplayName("Trả về 0 khi loại phòng không có phòng vật lý nào hoạt động")
  void shouldReturnZeroWhenNoActiveRooms() {
    UUID roomTypeId = UUID.randomUUID();
    when(roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId)).thenReturn(0L);

    int available =
        adapter.calculateAvailableQuantity(
            roomTypeId, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

    assertThat(available).isZero();
  }

  @Test
  @DisplayName(
      "Tính toán chính xác giá trị nhỏ nhất theo dải ngày và chống tính trùng phòng bị chặn")
  void shouldCalculateMinAvailableWithoutDuplicateBlocks() {
    UUID roomTypeId = UUID.randomUUID();
    UUID physicalRoom1 = UUID.randomUUID();
    UUID physicalRoom2 = UUID.randomUUID();

    // Tổng 10 phòng
    when(roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId)).thenReturn(10L);

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

    when(roomBlockJpaRepository.findActiveBlocksInRange(eq(roomTypeId), any(), any()))
        .thenReturn(List.of(block1, block2, block3));

    // Đơn đặt (held + confirmed):
    // Đêm 10/11: 3 phòng
    // Đêm 11/11: 5 phòng
    DailyReservationOccupancy occ1 = mock(DailyReservationOccupancy.class);
    when(occ1.getStayDate()).thenReturn(LocalDate.of(2026, 11, 10));
    when(occ1.getTotalQuantity()).thenReturn(3L);

    DailyReservationOccupancy occ2 = mock(DailyReservationOccupancy.class);
    when(occ2.getStayDate()).thenReturn(LocalDate.of(2026, 11, 11));
    when(occ2.getTotalQuantity()).thenReturn(5L);

    when(reservationJpaRepository.sumOccupancyByDateRange(eq(roomTypeId), any(), any(), any()))
        .thenReturn(List.of(occ1, occ2));

    // Đêm 10/11: total=10, blocked=1 (room1), occupied=3 => available = 10 - 1 - 3 = 6
    // Đêm 11/11: total=10, blocked=2 (room1, room2), occupied=5 => available = 10 - 2 - 5 = 3
    // Min available = min(6, 3) = 3
    int available =
        adapter.calculateAvailableQuantity(
            roomTypeId, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

    assertThat(available).isEqualTo(3);
  }
}

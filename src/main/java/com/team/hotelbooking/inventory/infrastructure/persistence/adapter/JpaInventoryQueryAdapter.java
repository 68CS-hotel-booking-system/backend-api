package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.DailyReservationOccupancy;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaInventoryQueryAdapter implements InventoryQueryPort {

  private final RoomJpaRepository roomJpaRepository;
  private final RoomBlockJpaRepository roomBlockJpaRepository;
  private final InventoryReservationJpaRepository reservationJpaRepository;

  @Override
  public int calculateAvailableQuantity(
      UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {

    long totalRooms = roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId);
    if (totalRooms <= 0) {
      return 0;
    }

    Instant now = Instant.now();

    // 1. Lấy tất cả room blocks còn ACTIVE giao thoa với khoảng lưu trú [checkInDate, checkOutDate)
    List<RoomBlockJpaEntity> activeBlocks =
        roomBlockJpaRepository.findActiveBlocksInRange(roomTypeId, checkInDate, checkOutDate);

    // 2. Lấy tổng số lượng phòng đang giữ (HELD hợp lệ) và đã xác nhận (CONFIRMED) gom theo ngày
    List<DailyReservationOccupancy> occupancyList =
        reservationJpaRepository.sumOccupancyByDateRange(
            roomTypeId, checkInDate, checkOutDate, now);

    Map<LocalDate, Long> occupancyByDate =
        occupancyList.stream()
            .collect(
                Collectors.toMap(
                    DailyReservationOccupancy::getStayDate,
                    DailyReservationOccupancy::getTotalQuantity,
                    Long::sum));

    int minAvailable = (int) totalRooms;

    // 3. Tính toán khả dụng từng đêm trong bộ nhớ (O(days))
    for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
      final LocalDate currentDate = date;

      // Đếm số phòng bị chặn (chống tính trùng phòng vật lý nếu có nhiều lịch chồng lặp theo IV-03)
      Set<UUID> blockedPhysicalRooms = new HashSet<>();
      int unassignedBlockedCount = 0;

      for (RoomBlockJpaEntity block : activeBlocks) {
        if (!currentDate.isBefore(block.getStartDate())
            && !currentDate.isAfter(block.getEndDate())) {
          if (block.getRoomId() != null) {
            blockedPhysicalRooms.add(block.getRoomId());
          } else {
            unassignedBlockedCount++;
          }
        }
      }
      int totalBlocked = blockedPhysicalRooms.size() + unassignedBlockedCount;

      long occupied = occupancyByDate.getOrDefault(currentDate, 0L);

      int availableForNight = Math.max(0, (int) (totalRooms - totalBlocked - occupied));
      if (availableForNight < minAvailable) {
        minAvailable = availableForNight;
      }
      if (minAvailable == 0) {
        break;
      }
    }

    return minAvailable;
  }
}

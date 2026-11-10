package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.domain.model.RoomBlockSpan;
import com.team.hotelbooking.inventory.domain.service.NightlyAvailabilityCalculator;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.DailyReservationOccupancy;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomTypeDailyOccupancy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaInventoryQueryAdapter implements InventoryQueryPort {

  private final HotelDirectory hotelDirectory;
  private final RoomBlockJpaRepository roomBlockJpaRepository;
  private final InventoryReservationJpaRepository reservationJpaRepository;
  private final Clock clock;

  @Override
  public int calculateAvailableQuantity(
      UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {

    long totalRooms = hotelDirectory.countActiveRoomsByRoomTypeId(roomTypeId);
    if (totalRooms <= 0) {
      return 0;
    }

    Instant now = clock.instant();

    // 1. Room blocks ACTIVE giao với khoảng lưu trú [checkInDate, checkOutDate)
    List<RoomBlockSpan> activeBlocks =
        roomBlockJpaRepository
            .findActiveBlocksInRange(roomTypeId, checkInDate, checkOutDate)
            .stream()
            .map(JpaInventoryQueryAdapter::toSpan)
            .toList();

    // 2. Phòng đang giữ (HELD hợp lệ) và đã xác nhận (CONFIRMED) gom theo ngày
    Map<LocalDate, Long> occupancyByDate =
        reservationJpaRepository
            .sumOccupancyByDateRange(roomTypeId, checkInDate, checkOutDate, now)
            .stream()
            .collect(
                Collectors.toMap(
                    DailyReservationOccupancy::getStayDate,
                    DailyReservationOccupancy::getTotalQuantity,
                    Long::sum));

    // 3. Khả dụng nhỏ nhất qua mọi đêm, cùng công thức với tìm kiếm theo lô
    return NightlyAvailabilityCalculator.minAvailable(
        totalRooms, activeBlocks, occupancyByDate, checkInDate, checkOutDate);
  }

  @Override
  public Map<UUID, Integer> calculateMinAvailableQuantities(
      Map<UUID, Long> operationalRoomsByRoomType, LocalDate checkInDate, LocalDate checkOutDate) {
    if (operationalRoomsByRoomType.isEmpty()) {
      return Map.of();
    }

    Instant now = clock.instant();

    // Hai truy vấn cho cả lô: lịch chặn và phòng đang giữ/đã xác nhận.
    Map<UUID, List<RoomBlockSpan>> blocksByRoomType =
        roomBlockJpaRepository
            .findActiveBlocksInRangeForRoomTypes(
                operationalRoomsByRoomType.keySet(), checkInDate, checkOutDate)
            .stream()
            .collect(
                Collectors.groupingBy(
                    RoomBlockJpaEntity::getRoomTypeId,
                    Collectors.mapping(JpaInventoryQueryAdapter::toSpan, Collectors.toList())));

    Map<UUID, Map<LocalDate, Long>> occupancyByRoomType = new HashMap<>();
    for (RoomTypeDailyOccupancy row :
        reservationJpaRepository.sumOccupancyByRoomTypeAndDate(
            operationalRoomsByRoomType.keySet(), checkInDate, checkOutDate, now)) {
      occupancyByRoomType
          .computeIfAbsent(row.getRoomTypeId(), id -> new HashMap<>())
          .merge(row.getStayDate(), row.getTotalQuantity(), Long::sum);
    }

    Map<UUID, Integer> result = new HashMap<>();
    operationalRoomsByRoomType.forEach(
        (roomTypeId, operationalRooms) ->
            result.put(
                roomTypeId,
                NightlyAvailabilityCalculator.minAvailable(
                    operationalRooms,
                    blocksByRoomType.getOrDefault(roomTypeId, List.of()),
                    occupancyByRoomType.getOrDefault(roomTypeId, Map.of()),
                    checkInDate,
                    checkOutDate)));
    return result;
  }

  private static RoomBlockSpan toSpan(RoomBlockJpaEntity block) {
    return new RoomBlockSpan(block.getRoomId(), block.getStartDate(), block.getEndDate());
  }
}

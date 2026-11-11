package com.team.hotelbooking.inventory.domain.service;

import com.team.hotelbooking.inventory.domain.model.RoomBlockSpan;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * IV-03, dùng chung cho tra phòng trống một khách sạn và tìm kiếm theo lô: khả dụng một đêm = phòng
 * đang phục vụ − phòng vật lý bị chặn (mỗi phòng đếm một lần dù có nhiều lịch chồng nhau) − lịch
 * chặn chưa gán phòng − phòng đang giữ hợp lệ/đã xác nhận. Kết quả là số nhỏ nhất qua mọi đêm của
 * [checkIn, checkOut), không âm.
 */
public final class NightlyAvailabilityCalculator {

  private NightlyAvailabilityCalculator() {}

  public static int minAvailable(
      long operationalRooms,
      Collection<RoomBlockSpan> blocks,
      Map<LocalDate, Long> occupiedByNight,
      LocalDate checkIn,
      LocalDate checkOut) {
    if (operationalRooms <= 0) {
      return 0;
    }

    int minAvailable = (int) operationalRooms;
    for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
      Set<UUID> blockedPhysicalRooms = new HashSet<>();
      int unassignedBlocks = 0;
      for (RoomBlockSpan block : blocks) {
        if (!block.covers(night)) {
          continue;
        }
        if (block.roomId() != null) {
          blockedPhysicalRooms.add(block.roomId());
        } else {
          unassignedBlocks++;
        }
      }

      long occupied = occupiedByNight.getOrDefault(night, 0L);
      int available =
          (int)
              Math.max(
                  0L, operationalRooms - blockedPhysicalRooms.size() - unassignedBlocks - occupied);
      minAvailable = Math.min(minAvailable, available);
      if (minAvailable == 0) {
        break;
      }
    }
    return minAvailable;
  }
}

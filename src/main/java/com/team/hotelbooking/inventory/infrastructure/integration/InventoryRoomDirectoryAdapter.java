package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.hotel.RoomInventoryDirectory;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomAssignmentJpaRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryRoomDirectoryAdapter implements RoomInventoryDirectory {
  private final HotelCatalogPort hotels;
  private final InventoryQueryPort inventory;
  private final RoomAssignmentJpaRepository assignments;
  private final Clock clock;

  @Override
  public void lockRoomType(UUID hotelId, UUID roomTypeId) {
    if (hotels.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)).size() != 1) {
      throw new RoomChangeConflict("Không tìm thấy loại phòng cần cập nhật.");
    }
  }

  @Override
  public void requireAllocatableAfterDeactivation(UUID roomTypeId, UUID roomId) {
    LocalDate today = BusinessTime.today(clock);
    LocalDate end = LocalDate.of(9999, 12, 31);
    if (!assignments.findActiveByRoomIdOverlapping(roomId, today, end).isEmpty()
        || !inventory.canAllocateAfterAssignment(roomTypeId, today, end)) {
      throw new RoomChangeConflict(
          "Không thể ngừng phục vụ phòng vì ảnh hưởng đến đơn đang giữ hoặc đã xác nhận.");
    }
  }
}

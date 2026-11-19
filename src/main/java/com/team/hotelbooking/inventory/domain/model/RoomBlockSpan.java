package com.team.hotelbooking.inventory.domain.model;

import java.time.LocalDate;
import java.util.UUID;

// Một lịch chặn ACTIVE: roomId null là chặn số lượng chưa gán phòng; endDate tính cả ngày cuối.
public record RoomBlockSpan(UUID roomId, LocalDate startDate, LocalDate endDate) {

  public boolean covers(LocalDate night) {
    return !night.isBefore(startDate) && !night.isAfter(endDate);
  }
}

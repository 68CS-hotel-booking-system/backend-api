package com.team.hotelbooking.inventory.application.port;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface InventoryQueryPort {
  int calculateAvailableQuantity(UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate);

  // Theo lô cho tìm kiếm: cùng công thức IV-03 với calculateAvailableQuantity, nhưng số phòng đang
  // phục vụ do bên gọi truyền vào (đã lấy theo lô từ hotel). Kết quả: loại phòng → khả dụng nhỏ
  // nhất qua mọi đêm.
  Map<UUID, Integer> calculateMinAvailableQuantities(
      Map<UUID, Long> operationalRoomsByRoomType, LocalDate checkInDate, LocalDate checkOutDate);

  boolean canAllocateAfterAssignment(
      UUID roomTypeId, LocalDate startDate, LocalDate endDateExclusive);

  boolean canAllocateWithCandidateBlock(
      UUID roomTypeId,
      UUID candidateBlockIdToReplace,
      UUID roomId,
      LocalDate blockStartDate,
      LocalDate blockEndDate);
}

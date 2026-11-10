package com.team.hotelbooking.inventory.application.port;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

// Giá kỳ lưu trú theo lô, lấy từ pricing; inventory không tự tính giá.
public interface StayPricingPort {

  // Loại phòng không đang bán (theo pricing) không có trong map.
  Map<UUID, RoomTypeStayPrice> findStayPrices(
      Collection<UUID> roomTypeIds, LocalDate checkIn, LocalDate checkOut);

  // sellable = mọi đêm có giá hợp lệ (> 0); totalAmount là tổng giá một phòng cho kỳ lưu trú.
  record RoomTypeStayPrice(UUID roomTypeId, boolean sellable, long totalAmount) {}
}

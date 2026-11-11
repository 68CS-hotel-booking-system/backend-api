package com.team.hotelbooking.pricing.application.port;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingHotelPort {

  boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId);

  Optional<HotelPricingSnapshot> findActiveHotel(UUID hotelId);

  Optional<RoomTypePricingSnapshot> findActiveRoomType(UUID roomTypeId, UUID hotelId);

  // Theo lô cho tìm kiếm: chỉ loại phòng đang bán thuộc khách sạn ACTIVE, giống điều kiện báo giá.
  List<RoomTypePricingSnapshot> findActiveRoomTypes(Collection<UUID> roomTypeIds);

  record HotelPricingSnapshot(UUID id, LocalTime checkInTime, LocalTime checkOutTime) {}

  record RoomTypePricingSnapshot(
      UUID id,
      UUID hotelId,
      String name,
      int maxAdults,
      int maxChildren,
      int maxOccupancy,
      long basePrice) {}

  // Mức quyền quản lý của người gọi và trạng thái hotel; rỗng khi hotel không tồn tại.
  Optional<HotelManagement> findManagementAccess(UUID hotelId, UUID actorUserId, boolean admin);

  record HotelManagement(Level level, HotelStatus status) {}

  enum Level {
    ADMIN,
    OWNER,
    MANAGER,
    RECEPTIONIST,
    NONE
  }

  enum HotelStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    LOCKED
  }
}

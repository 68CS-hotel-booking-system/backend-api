package com.team.hotelbooking.hotel;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HotelDirectory {

  // Phục vụ module booking & pricing
  Optional<HotelInfo> findById(UUID hotelId);

  // Phục vụ module payment
  boolean existsById(UUID hotelId);

  Optional<UUID> findOwnerUserId(UUID hotelId);

  // Phục vụ module pricing
  boolean isHotelActive(UUID hotelId);

  boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId);

  Optional<RoomTypeSummaryView> findActiveRoomType(UUID roomTypeId, UUID hotelId);

  // Mức quyền quản lý của người gọi với hotel và trạng thái hotel, cùng quy tắc với PATCH hotel.
  // Rỗng khi hotel không tồn tại; module gọi tự đổi kết quả thành mã lỗi của mình.
  Optional<ManagementAccessView> findManagementAccess(
      UUID hotelId, UUID actorUserId, boolean admin);

  // Phục vụ module inventory & pricing: kiểm tra quyền quản lý khách sạn
  boolean hasManagementAccess(UUID actorId, UUID hotelId);

  // Phục vụ module inventory
  Optional<HotelSummaryView> findHotelSummary(UUID hotelId);

  List<UUID> findRoomTypeIdsByHotelId(UUID hotelId);

  List<RoomTypeSummaryView> findActiveRoomTypes(UUID hotelId);

  List<RoomTypeSummaryView> lockActiveRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds);

  List<RoomTypeSummaryView> lockRoomTypesInHotel(UUID hotelId, Collection<UUID> roomTypeIds);

  long countActiveRoomsByRoomTypeId(UUID roomTypeId);

  // Phục vụ tìm kiếm (inventory) và tra giá theo lô (pricing): mỗi hàm một truy vấn cho cả danh
  // sách.
  // Chỉ trả khách sạn ACTIVE (HT-05), có đủ MỌI tiện ích khách sạn trong amenityIds.
  List<SearchableHotelView> findActiveHotelsForSearch(HotelSearchFilter filter);

  List<RoomTypeCatalogView> findActiveRoomTypesByHotelIds(Collection<UUID> hotelIds);

  // Loại phòng đang bán thuộc khách sạn ACTIVE, cùng điều kiện với findActiveHotel +
  // findActiveRoomType.
  List<RoomTypeCatalogView> findActiveRoomTypesByIds(Collection<UUID> roomTypeIds);

  // Phòng vật lý đang phục vụ tại thời điểm at; loại phòng không có phòng nào không có trong map.
  Map<UUID, Long> countOperationalRoomsByRoomTypeIds(Collection<UUID> roomTypeIds, Instant at);

  List<PhysicalRoomView> findPhysicalRoomsByRoomTypeId(UUID roomTypeId);

  List<PhysicalRoomView> findPhysicalRoomsByRoomTypeIds(Collection<UUID> roomTypeIds);

  List<PhysicalRoomView> findPhysicalRoomsByHotelId(UUID hotelId);

  Optional<PhysicalRoomView> findPhysicalRoomByIdAndHotelId(UUID roomId, UUID hotelId);

  Optional<PhysicalRoomView> updateRoomReadiness(
      UUID roomId, UUID hotelId, String readiness, UUID actorUserId, Instant updatedAt);

  // DTO Records dùng chung
  record HotelInfo(
      UUID id,
      UUID ownerId,
      String name,
      LocalTime checkInTime,
      LocalTime checkOutTime,
      boolean active) {}

  record HotelSummaryView(UUID id, boolean active, boolean verified) {}

  record ManagementAccessView(ManagementLevel level, HotelStatusView status) {}

  enum ManagementLevel {
    ADMIN,
    OWNER,
    MANAGER,
    RECEPTIONIST,
    NONE
  }

  enum HotelStatusView {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    LOCKED
  }

  record HotelSearchFilter(
      UUID cityId, String citySlug, Integer minStarRating, Set<UUID> amenityIds) {

    public HotelSearchFilter {
      amenityIds = amenityIds == null ? Set.of() : Set.copyOf(amenityIds);
    }
  }

  record SearchableHotelView(
      UUID id,
      String name,
      String slug,
      String address,
      Short starRating,
      CityView city,
      List<AmenityView> amenities) {

    public SearchableHotelView {
      amenities = List.copyOf(amenities);
    }
  }

  record CityView(UUID id, String name, String slug) {}

  record AmenityView(UUID id, String code, String name) {}

  record RoomTypeCatalogView(
      UUID id,
      UUID hotelId,
      String name,
      int maxOccupancy,
      int maxAdults,
      int maxChildren,
      long basePrice) {}

  record RoomTypeSummaryView(
      UUID id, String name, int maxOccupancy, int maxAdults, int maxChildren, long basePrice) {}

  record PhysicalRoomView(
      UUID id,
      UUID hotelId,
      UUID roomTypeId,
      String roomNumber,
      String floor,
      String readiness,
      Instant operationalFrom,
      Instant operationalUntil) {

    public PhysicalRoomView(
        UUID id,
        UUID hotelId,
        UUID roomTypeId,
        String roomNumber,
        String floor,
        Instant operationalFrom,
        Instant operationalUntil) {
      this(id, hotelId, roomTypeId, roomNumber, floor, "READY", operationalFrom, operationalUntil);
    }
  }
}

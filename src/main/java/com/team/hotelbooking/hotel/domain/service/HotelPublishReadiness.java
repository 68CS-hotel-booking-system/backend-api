package com.team.hotelbooking.hotel.domain.service;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelPublishRequirement;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HT-03: khách sạn chỉ được mở bán khi có tên, địa chỉ, thông tin liên hệ và ít nhất một loại phòng
 * đang bán, có sức chứa hợp lệ (HT-04: số nguyên dương, người lớn không vượt sức chứa) và có phòng
 * vật lý đang phục vụ. Áp dụng cho mọi lần chuyển sang ACTIVE (approve và resume).
 */
public final class HotelPublishReadiness {

  private HotelPublishReadiness() {}

  public static List<HotelPublishRequirement> unmetRequirements(
      Hotel hotel, Collection<RoomType> roomTypes, Map<UUID, Long> operationalRoomsByRoomType) {
    List<HotelPublishRequirement> unmet = new ArrayList<>();

    if (isBlank(hotel.name())) {
      unmet.add(HotelPublishRequirement.NAME);
    }
    if (isBlank(hotel.address())) {
      unmet.add(HotelPublishRequirement.ADDRESS);
    }
    if (isBlank(hotel.contactPhone()) && isBlank(hotel.contactEmail())) {
      unmet.add(HotelPublishRequirement.CONTACT);
    }

    List<RoomType> sellable = roomTypes.stream().filter(HotelPublishReadiness::sellable).toList();
    if (sellable.isEmpty()) {
      unmet.add(HotelPublishRequirement.ROOM_TYPE);
    } else if (sellable.stream()
        .noneMatch(roomType -> operationalRoomsByRoomType.getOrDefault(roomType.id(), 0L) > 0)) {
      unmet.add(HotelPublishRequirement.OPERATIONAL_ROOM);
    }

    return List.copyOf(unmet);
  }

  private static boolean sellable(RoomType roomType) {
    return roomType.active()
        && roomType.maxAdults() >= 1
        && roomType.maxChildren() >= 0
        && roomType.maxOccupancy() >= 1
        && roomType.maxAdults() <= roomType.maxOccupancy()
        && roomType.maxChildren() <= roomType.maxOccupancy();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

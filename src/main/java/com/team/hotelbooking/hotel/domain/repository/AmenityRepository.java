package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AmenityRepository {

  List<Amenity> findAll();

  List<Amenity> findByCategory(Amenity.Category category);

  List<Amenity> findAllByIds(Collection<UUID> amenityIds);

  // Các danh sách gắn với hotel/room_type đều sắp theo tên rồi id.
  List<Amenity> findByHotelId(UUID hotelId);

  List<Amenity> findByRoomTypeId(UUID roomTypeId);

  // Trả map đủ key cho mọi roomTypeId truyền vào; loại phòng chưa có tiện ích có list rỗng.
  Map<UUID, List<Amenity>> findByRoomTypeIds(Collection<UUID> roomTypeIds);

  void replaceHotelAmenities(UUID hotelId, Set<UUID> amenityIds);

  void replaceRoomTypeAmenities(UUID roomTypeId, Set<UUID> amenityIds);
}

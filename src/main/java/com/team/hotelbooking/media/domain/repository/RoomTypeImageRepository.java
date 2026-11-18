package com.team.hotelbooking.media.domain.repository;

import com.team.hotelbooking.media.domain.model.RoomTypeImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeImageRepository {
  Optional<RoomTypeImage> findLink(UUID hotelId, UUID roomTypeId, UUID mediaId);

  List<RoomTypeImage> findReadyImages(UUID hotelId, UUID roomTypeId);

  List<UUID> findLinkedRoomTypes(UUID hotelId, UUID mediaId);

  void add(RoomTypeImage image);
}

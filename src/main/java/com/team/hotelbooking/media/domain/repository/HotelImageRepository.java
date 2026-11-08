package com.team.hotelbooking.media.domain.repository;

import com.team.hotelbooking.media.domain.model.HotelImage;
import com.team.hotelbooking.media.domain.model.MediaObject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HotelImageRepository {

  List<HotelImage> findByHotelId(UUID hotelId);

  List<MediaObject> lockMedia(List<UUID> mediaIds);

  boolean isLinked(UUID hotelId, UUID mediaId);

  void replace(UUID hotelId, List<HotelImage> images, Instant createdAt);
}

package com.team.hotelbooking.media.domain.repository;

import com.team.hotelbooking.media.domain.model.HotelImageAlbum;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelImageAlbumRepository {

  List<HotelImageAlbum> findByHotelId(UUID hotelId);

  Optional<HotelImageAlbum> findByIdAndHotelId(UUID albumId, UUID hotelId);

  void add(HotelImageAlbum album);

  void update(HotelImageAlbum album);

  void deleteAndUngroupImages(UUID hotelId, UUID albumId);
}

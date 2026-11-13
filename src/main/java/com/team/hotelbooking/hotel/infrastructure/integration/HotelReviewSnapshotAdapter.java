package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.application.port.HotelReviewSnapshotPort;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot.AlbumSnapshot;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot.ImageSnapshot;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot.RoomSnapshot;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot.RoomTypeSnapshot;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.hotel.domain.repository.ScopedAmenityRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelReviewSnapshotAdapter implements HotelReviewSnapshotPort {
  private final RoomTypeRepository roomTypes;
  private final RoomJpaRepository rooms;
  private final AmenityRepository amenities;
  private final ScopedAmenityRepository scoped;
  private final JdbcTemplate jdbc;

  @Override
  public HotelReviewSnapshot capture(Hotel hotel) {
    UUID hotelId = hotel.id();
    return new HotelReviewSnapshot(
        hotel,
        amenities.findByHotelId(hotelId),
        scoped.findCustomByScope(hotelId, null),
        roomTypes.findByHotelId(hotelId).stream()
            .map(r -> new RoomTypeSnapshot(r, amenities.findByRoomTypeId(r.id())))
            .toList(),
        rooms.findAllByHotelIdOrdered(hotelId).stream()
            .map(
                r ->
                    new RoomSnapshot(
                        r.getId(),
                        r.getRoomTypeId(),
                        r.getRoomNumber(),
                        r.getFloor(),
                        r.getOperationalFrom(),
                        r.getOperationalUntil(),
                        scoped.findCatalogByRoomId(r.getId()),
                        scoped.findCustomByScope(hotelId, r.getId())))
            .toList(),
        jdbc.query(
            """
            SELECT i.media_object_id, i.album_id, i.display_order, i.is_cover, i.alt_text
            FROM hotel_images i JOIN media_objects m ON m.id = i.media_object_id
            WHERE i.hotel_id = ? AND m.status = 'READY'
            ORDER BY i.display_order, i.media_object_id
            """,
            (rs, row) ->
                new ImageSnapshot(
                    rs.getObject(1, UUID.class),
                    rs.getObject(2, UUID.class),
                    rs.getInt(3),
                    rs.getBoolean(4),
                    rs.getString(5)),
            hotelId),
        jdbc.query(
            """
            SELECT id, name, description, sort_order FROM hotel_image_albums
            WHERE hotel_id = ? ORDER BY sort_order, id
            """,
            (rs, row) ->
                new AlbumSnapshot(
                    rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getInt(4)),
            hotelId));
  }
}

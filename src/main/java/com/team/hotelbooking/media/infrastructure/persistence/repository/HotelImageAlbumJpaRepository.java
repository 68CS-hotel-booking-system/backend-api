package com.team.hotelbooking.media.infrastructure.persistence.repository;

import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageAlbumJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelImageAlbumJpaRepository
    extends JpaRepository<HotelImageAlbumJpaEntity, UUID> {

  List<HotelImageAlbumJpaEntity> findByHotelIdOrderBySortOrderAscIdAsc(UUID hotelId);

  Optional<HotelImageAlbumJpaEntity> findByIdAndHotelId(UUID albumId, UUID hotelId);
}

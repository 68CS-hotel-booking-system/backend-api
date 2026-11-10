package com.team.hotelbooking.media.infrastructure.persistence.repository;

import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageId;
import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelImageJpaRepository extends JpaRepository<HotelImageJpaEntity, HotelImageId> {

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update HotelImageJpaEntity i
      set i.albumId = null
      where i.id.hotelId = :hotelId
        and i.albumId = :albumId
      """)
  int ungroupImages(@Param("hotelId") UUID hotelId, @Param("albumId") UUID albumId);
}

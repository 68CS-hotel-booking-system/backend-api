package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelAmenityJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelAmenityJpaRepository
    extends JpaRepository<HotelAmenityJpaEntity, HotelAmenityJpaEntity.Key> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from HotelAmenityJpaEntity a where a.id.hotelId = :hotelId")
  int deleteByHotelId(@Param("hotelId") UUID hotelId);
}

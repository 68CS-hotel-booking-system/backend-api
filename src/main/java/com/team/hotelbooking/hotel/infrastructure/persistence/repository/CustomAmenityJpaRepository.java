package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.CustomAmenityJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomAmenityJpaRepository extends JpaRepository<CustomAmenityJpaEntity, UUID> {
  @Query(
      "select a from CustomAmenityJpaEntity a where a.hotelId = :hotelId and (a.roomId = :roomId or (a.roomId is null and :roomId is null)) order by a.name, a.id")
  List<CustomAmenityJpaEntity> findByScope(
      @Param("hotelId") UUID hotelId, @Param("roomId") UUID roomId);

  @Query(
      "select a from CustomAmenityJpaEntity a where a.id = :id and a.hotelId = :hotelId and (a.roomId = :roomId or (a.roomId is null and :roomId is null))")
  Optional<CustomAmenityJpaEntity> findByIdAndScope(
      @Param("id") UUID id, @Param("hotelId") UUID hotelId, @Param("roomId") UUID roomId);

  @Query(
      "select (count(a) > 0) from CustomAmenityJpaEntity a where a.hotelId = :hotelId and (a.roomId = :roomId or (a.roomId is null and :roomId is null)) and a.normalizedName = :name and a.id <> :excludedId")
  boolean existsOtherName(
      @Param("hotelId") UUID hotelId,
      @Param("roomId") UUID roomId,
      @Param("name") String name,
      @Param("excludedId") UUID excludedId);
}

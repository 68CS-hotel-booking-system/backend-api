package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeJpaEntity, UUID> {

  Optional<RoomTypeJpaEntity> findByIdAndHotelId(UUID roomTypeId, UUID hotelId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select rt from RoomTypeJpaEntity rt where rt.id = :roomTypeId and rt.hotelId = :hotelId")
  Optional<RoomTypeJpaEntity> findByIdAndHotelIdForUpdate(
      @Param("roomTypeId") UUID roomTypeId, @Param("hotelId") UUID hotelId);

  List<RoomTypeJpaEntity> findAllByHotelIdAndActiveTrue(UUID hotelId);

  Optional<RoomTypeJpaEntity> findByIdAndHotelIdAndActiveTrue(UUID id, UUID hotelId);

  List<RoomTypeJpaEntity> findAllByHotelIdAndActiveTrueOrderByNameAscIdAsc(UUID hotelId);

  List<RoomTypeJpaEntity> findAllByHotelIdOrderByNameAscIdAsc(UUID hotelId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT rt FROM RoomTypeJpaEntity rt
      WHERE rt.hotelId = :hotelId
        AND rt.id IN :ids
        AND rt.active = true
      ORDER BY rt.id ASC
      """)
  List<RoomTypeJpaEntity> lockActiveByHotelIdAndIdInOrderByIdAsc(
      @Param("hotelId") UUID hotelId, @Param("ids") Collection<UUID> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT rt FROM RoomTypeJpaEntity rt
      WHERE rt.hotelId = :hotelId
        AND rt.id IN :ids
      ORDER BY rt.id ASC
      """)
  List<RoomTypeJpaEntity> lockByHotelIdAndIdInOrderByIdAsc(
      @Param("hotelId") UUID hotelId, @Param("ids") Collection<UUID> ids);
}

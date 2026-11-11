package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {

  Optional<RoomJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

  boolean existsByHotelIdAndRoomNumberAndIdNot(UUID hotelId, String roomNumber, UUID id);

  // Đang phục vụ: đã tới operationalFrom và chưa tới operationalUntil (nếu có).
  @Query(
      """
      SELECT r.roomTypeId, COUNT(r) FROM RoomJpaEntity r
      WHERE r.hotelId = :hotelId
        AND r.operationalFrom <= :at
        AND (r.operationalUntil IS NULL OR r.operationalUntil > :at)
      GROUP BY r.roomTypeId
      """)
  List<Object[]> countOperationalRoomsByRoomType(
      @Param("hotelId") UUID hotelId, @Param("at") Instant at);

  @Query(
      """
      SELECT COUNT(r) FROM RoomJpaEntity r
      WHERE r.roomTypeId = :roomTypeId
        AND (r.operationalUntil IS NULL OR r.operationalUntil > CURRENT_TIMESTAMP)
      """)
  long countByRoomTypeIdAndActiveTrue(@Param("roomTypeId") UUID roomTypeId);

  @Query(
      """
      SELECT r FROM RoomJpaEntity r
      WHERE r.roomTypeId = :roomTypeId
        AND (r.operationalUntil IS NULL OR r.operationalUntil > CURRENT_TIMESTAMP)
      """)
  List<RoomJpaEntity> findAllByRoomTypeIdAndActiveTrue(@Param("roomTypeId") UUID roomTypeId);

  List<RoomJpaEntity> findAllByRoomTypeIdOrderByIdAsc(UUID roomTypeId);

  List<RoomJpaEntity> findAllByRoomTypeIdIn(Collection<UUID> roomTypeIds);

  @Query(
      """
      select r from RoomJpaEntity r
      where r.hotelId = :hotelId
      order by r.floor asc nulls last, r.roomNumber asc, r.id asc
      """)
  List<RoomJpaEntity> findAllByHotelIdOrdered(@Param("hotelId") UUID hotelId);

  // Thứ tự nằm trong JPQL với NULLS LAST tường minh; Pageable truyền vào không được mang Sort.
  // floor/room_number là chuỗi nên sắp theo collation ("10" trước "2", "1001" trước "101").
  @Query(
      value =
          """
          select r from RoomJpaEntity r
          where r.hotelId = :hotelId
          order by r.floor asc nulls last, r.roomNumber asc, r.id asc
          """,
      countQuery = "select count(r) from RoomJpaEntity r where r.hotelId = :hotelId")
  Page<RoomJpaEntity> findManagementPage(@Param("hotelId") UUID hotelId, Pageable pageable);

  @Query(
      value =
          """
          select r from RoomJpaEntity r
          where r.hotelId = :hotelId and r.roomTypeId = :roomTypeId
          order by r.floor asc nulls last, r.roomNumber asc, r.id asc
          """,
      countQuery =
          """
          select count(r) from RoomJpaEntity r
          where r.hotelId = :hotelId and r.roomTypeId = :roomTypeId
          """)
  Page<RoomJpaEntity> findManagementPageByRoomType(
      @Param("hotelId") UUID hotelId, @Param("roomTypeId") UUID roomTypeId, Pageable pageable);
}

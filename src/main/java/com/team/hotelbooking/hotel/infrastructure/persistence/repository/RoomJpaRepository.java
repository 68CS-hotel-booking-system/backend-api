package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {

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

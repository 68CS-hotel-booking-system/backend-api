package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.AmenityJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmenityJpaRepository extends JpaRepository<AmenityJpaEntity, UUID> {

  List<AmenityJpaEntity> findAllByOrderByCategoryAscNameAscIdAsc();

  List<AmenityJpaEntity> findAllByCategoryOrderByNameAscIdAsc(AmenityJpaEntity.DbCategory category);

  @Query(
      """
      select a from AmenityJpaEntity a
      join HotelAmenityJpaEntity ha on ha.id.amenityId = a.id
      where ha.id.hotelId = :hotelId
      order by a.name asc, a.id asc
      """)
  List<AmenityJpaEntity> findAllByHotelId(@Param("hotelId") UUID hotelId);

  @Query(
      "select a from AmenityJpaEntity a join RoomAmenityJpaEntity ra on ra.id.amenityId = a.id where ra.id.roomId = :roomId order by a.name, a.id")
  List<AmenityJpaEntity> findAllByPhysicalRoomId(@Param("roomId") UUID roomId);

  // Mỗi dòng là [roomTypeId, AmenityJpaEntity]; sắp theo tên trong DB để cùng collation với
  // danh sách tiện ích của khách sạn.
  @Query(
      """
      select ra.id.roomTypeId, a from AmenityJpaEntity a
      join RoomTypeAmenityJpaEntity ra on ra.id.amenityId = a.id
      where ra.id.roomTypeId in :roomTypeIds
      order by a.name asc, a.id asc
      """)
  List<Object[]> findAllByRoomTypeIds(@Param("roomTypeIds") Collection<UUID> roomTypeIds);
}

package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelJpaRepository
    extends JpaRepository<HotelJpaEntity, UUID>, JpaSpecificationExecutor<HotelJpaEntity> {

  Optional<HotelJpaEntity> findByIdAndOwnerId(UUID hotelId, UUID ownerId);

  Optional<HotelJpaEntity> findByIdAndStatus(UUID hotelId, HotelJpaEntity.DbStatus status);

  Page<HotelJpaEntity> findByOwnerId(UUID ownerId, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select h from HotelJpaEntity h where h.id = :hotelId")
  Optional<HotelJpaEntity> findByIdForUpdate(@Param("hotelId") UUID hotelId);

  // hotel_members chưa có entity riêng; chỉ cần các vai trò đang hoạt động của user tại hotel.
  @Query(
      value =
          """
          SELECT role::text FROM hotel_members
          WHERE hotel_id = :hotelId AND user_id = :userId AND is_active
          """,
      nativeQuery = true)
  List<String> findActiveMemberRoles(@Param("hotelId") UUID hotelId, @Param("userId") UUID userId);

  default Optional<HotelJpaEntity> findByIdAndActiveTrue(UUID id) {
    return findByIdAndStatus(id, HotelJpaEntity.DbStatus.ACTIVE);
  }
}

package com.team.hotelbooking.booking.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, UUID> {

  Optional<BookingJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

  Optional<BookingJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

  Page<BookingJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

  Page<BookingJpaEntity> findByCustomerIdAndStatus(
      UUID customerId, BookingJpaEntity.DbStatus status, Pageable pageable);

  Page<BookingJpaEntity> findByHotelId(UUID hotelId, Pageable pageable);

  Page<BookingJpaEntity> findByHotelIdAndStatus(
      UUID hotelId, BookingJpaEntity.DbStatus status, Pageable pageable);

  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      """
      UPDATE BookingJpaEntity b
      SET b.status = :status, b.updatedAt = :updatedAt
      WHERE b.id = :bookingId
      """)
  int updateStatus(
      @org.springframework.data.repository.query.Param("bookingId") UUID bookingId,
      @org.springframework.data.repository.query.Param("status") BookingJpaEntity.DbStatus status,
      @org.springframework.data.repository.query.Param("updatedAt") java.time.Instant updatedAt);
}

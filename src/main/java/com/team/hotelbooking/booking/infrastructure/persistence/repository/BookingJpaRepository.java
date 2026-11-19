package com.team.hotelbooking.booking.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, UUID> {

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select b from BookingJpaEntity b where b.id = :id")
  Optional<BookingJpaEntity> lockById(
      @org.springframework.data.repository.query.Param("id") UUID id);

  Optional<BookingJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

  Optional<BookingJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

  Page<BookingJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

  Page<BookingJpaEntity> findByCustomerIdAndStatus(
      UUID customerId, BookingJpaEntity.DbStatus status, Pageable pageable);

  @Query(
      """
      select b from BookingJpaEntity b
      where b.customerId = :customerId and b.status = :pending
        and (b.expiresAt is null or b.expiresAt > :now)
      """)
  Page<BookingJpaEntity> findPendingCustomerBookings(
      @Param("customerId") UUID customerId,
      @Param("pending") BookingJpaEntity.DbStatus pending,
      @Param("now") Instant now,
      Pageable pageable);

  @Query(
      """
      select b from BookingJpaEntity b
      where b.customerId = :customerId
        and (b.status = :expired or (b.status = :pending and b.expiresAt <= :now))
      """)
  Page<BookingJpaEntity> findExpiredCustomerBookings(
      @Param("customerId") UUID customerId,
      @Param("pending") BookingJpaEntity.DbStatus pending,
      @Param("expired") BookingJpaEntity.DbStatus expired,
      @Param("now") Instant now,
      Pageable pageable);

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

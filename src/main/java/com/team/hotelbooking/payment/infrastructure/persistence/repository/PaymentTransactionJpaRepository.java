package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity.Status;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionJpaRepository
    extends JpaRepository<PaymentTransactionJpaEntity, UUID> {
  Optional<PaymentTransactionJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

  java.util.List<PaymentTransactionJpaEntity> findAllByBookingIdAndStatusOrderByIdAsc(
      UUID bookingId, Status status);

  @Query("select p.bookingId from PaymentTransactionJpaEntity p where p.id = :id")
  Optional<UUID> findBookingIdById(@Param("id") UUID id);

  Optional<PaymentTransactionJpaEntity> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
      UUID bookingId, Status status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PaymentTransactionJpaEntity p where p.id = :id")
  Optional<PaymentTransactionJpaEntity> lockById(@Param("id") UUID id);
}

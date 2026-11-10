package com.team.hotelbooking.booking.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingItemJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemJpaRepository extends JpaRepository<BookingItemJpaEntity, UUID> {

  List<BookingItemJpaEntity> findAllByBookingIdOrderByIdAsc(UUID bookingId);
}

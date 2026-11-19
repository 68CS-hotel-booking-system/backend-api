package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import com.team.hotelbooking.inventory.infrastructure.persistence.entity.BookingStayOperationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingStayOperationJpaRepository
    extends JpaRepository<BookingStayOperationJpaEntity, UUID> {

  Optional<BookingStayOperationJpaEntity> findByBookingIdAndHotelId(UUID bookingId, UUID hotelId);
}

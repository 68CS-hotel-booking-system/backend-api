package com.team.hotelbooking.booking.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingNightPriceIdEmbeddable;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingNightPriceJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingNightPriceJpaRepository
    extends JpaRepository<BookingNightPriceJpaEntity, BookingNightPriceIdEmbeddable> {

  List<BookingNightPriceJpaEntity> findAllByIdBookingItemIdInOrderByIdStayDateAsc(
      Collection<UUID> bookingItemIds);
}

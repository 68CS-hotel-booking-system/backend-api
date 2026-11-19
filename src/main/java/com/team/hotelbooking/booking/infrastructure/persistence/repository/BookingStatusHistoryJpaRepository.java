package com.team.hotelbooking.booking.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingStatusHistoryJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusHistoryJpaRepository
    extends JpaRepository<BookingStatusHistoryJpaEntity, UUID> {}

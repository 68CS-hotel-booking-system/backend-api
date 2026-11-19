package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessHistoryJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelFinancialAccessHistoryJpaRepository
    extends JpaRepository<HotelFinancialAccessHistoryJpaEntity, UUID> {

  List<HotelFinancialAccessHistoryJpaEntity> findAllByFinancialAccessIdOrderByVersionDesc(
      UUID financialAccessId);
}

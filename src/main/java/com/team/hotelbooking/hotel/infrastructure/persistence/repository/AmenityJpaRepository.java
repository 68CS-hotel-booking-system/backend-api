package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.AmenityJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityJpaRepository extends JpaRepository<AmenityJpaEntity, UUID> {

  List<AmenityJpaEntity> findAllByOrderByCategoryAscNameAscIdAsc();

  List<AmenityJpaEntity> findAllByCategoryOrderByNameAscIdAsc(AmenityJpaEntity.DbCategory category);
}

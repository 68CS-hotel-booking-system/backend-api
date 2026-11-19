package com.team.hotelbooking.pricing.infrastructure.persistence.mapper;

import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateIdEmbeddable;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomRatePersistenceMapper {

  public RoomRate toDomain(RoomRateJpaEntity entity) {
    return new RoomRate(
        entity.getId().getRoomTypeId(),
        entity.getId().getRateDate(),
        entity.getAmount(),
        entity.getCurrency(),
        entity.getUpdatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public RoomRateJpaEntity toEntity(RoomRate domain) {
    RoomRateJpaEntity entity = new RoomRateJpaEntity();
    entity.setId(new RoomRateIdEmbeddable(domain.roomTypeId(), domain.rateDate()));
    entity.setAmount(domain.amount());
    entity.setCurrency(domain.currency());
    entity.setUpdatedByUserId(domain.updatedByUserId());
    entity.setCreatedAt(domain.createdAt());
    entity.setUpdatedAt(domain.updatedAt());
    return entity;
  }
}

package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelStatusHistoryJpaEntity;

public final class HotelStatusHistoryPersistenceMapper {

  private HotelStatusHistoryPersistenceMapper() {}

  public static HotelStatusHistoryJpaEntity toNewEntity(HotelStatusChange change) {
    HotelStatusHistoryJpaEntity entity = new HotelStatusHistoryJpaEntity();
    entity.setId(change.id());
    entity.setHotelId(change.hotelId());
    entity.setFromStatus(HotelPersistenceMapper.toDbStatus(change.fromStatus()));
    entity.setToStatus(HotelPersistenceMapper.toDbStatus(change.toStatus()));
    entity.setReason(change.reason());
    entity.setChangedByUserId(change.changedByUserId());
    entity.setChangedAt(change.changedAt());
    return entity;
  }

  public static HotelStatusChange toDomain(HotelStatusHistoryJpaEntity entity) {
    return new HotelStatusChange(
        entity.getId(),
        entity.getHotelId(),
        Hotel.Status.valueOf(entity.getFromStatus().name()),
        Hotel.Status.valueOf(entity.getToStatus().name()),
        entity.getReason(),
        entity.getChangedByUserId(),
        entity.getChangedAt());
  }
}

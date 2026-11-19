package com.team.hotelbooking.hotel.infrastructure.persistence.mapper;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;

public final class HotelPersistenceMapper {

  private HotelPersistenceMapper() {}

  public static HotelJpaEntity toNewEntity(Hotel hotel) {
    HotelJpaEntity entity = new HotelJpaEntity();

    entity.setId(hotel.id());
    entity.setOwnerId(hotel.ownerId());
    entity.setCityId(hotel.cityId());
    entity.setName(hotel.name());
    entity.setSlug(hotel.slug());
    entity.setDescription(hotel.description());
    entity.setAddress(hotel.address());
    entity.setContactPhone(hotel.contactPhone());
    entity.setContactEmail(hotel.contactEmail());
    entity.setLatitude(hotel.latitude());
    entity.setLongitude(hotel.longitude());
    entity.setStarRating(hotel.starRating());
    entity.setCheckInTime(hotel.checkInTime());
    entity.setCheckOutTime(hotel.checkOutTime());
    entity.setStatus(HotelJpaEntity.DbStatus.valueOf(hotel.status().name()));
    entity.setCreatedAt(hotel.createdAt());
    entity.setUpdatedAt(hotel.updatedAt());

    // Để version null cho entity mới; Hibernate quản lý @Version.
    return entity;
  }

  // Chỉ chép các field sửa được; giữ nguyên id, Owner, thành phố, slug, trạng thái, createdAt và
  // version để @Version tự tăng khi flush.
  public static void applyDetails(HotelJpaEntity entity, Hotel hotel) {
    entity.setName(hotel.name());
    entity.setDescription(hotel.description());
    entity.setAddress(hotel.address());
    entity.setContactPhone(hotel.contactPhone());
    entity.setContactEmail(hotel.contactEmail());
    entity.setLatitude(hotel.latitude());
    entity.setLongitude(hotel.longitude());
    entity.setStarRating(hotel.starRating());
    entity.setCheckInTime(hotel.checkInTime());
    entity.setCheckOutTime(hotel.checkOutTime());
    entity.setUpdatedAt(hotel.updatedAt());
  }

  public static Hotel toDomain(HotelJpaEntity entity) {
    return new Hotel(
        entity.getId(),
        entity.getOwnerId(),
        entity.getCityId(),
        entity.getName(),
        entity.getSlug(),
        entity.getDescription(),
        entity.getAddress(),
        entity.getContactPhone(),
        entity.getContactEmail(),
        entity.getLatitude(),
        entity.getLongitude(),
        entity.getStarRating(),
        entity.getCheckInTime(),
        entity.getCheckOutTime(),
        Hotel.Status.valueOf(entity.getStatus().name()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }

  public static HotelJpaEntity.DbStatus toDbStatus(Hotel.Status status) {
    return HotelJpaEntity.DbStatus.valueOf(status.name());
  }
}

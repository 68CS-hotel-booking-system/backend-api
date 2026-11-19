package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.CustomAmenity;
import com.team.hotelbooking.hotel.domain.repository.ScopedAmenityRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomAmenityJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.AmenityPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.CustomAmenityPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.AmenityJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.CustomAmenityJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomAmenityJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaScopedAmenityRepositoryAdapter implements ScopedAmenityRepository {
  private final AmenityJpaRepository catalog;
  private final RoomAmenityJpaRepository roomAmenities;
  private final CustomAmenityJpaRepository custom;

  @Override
  public List<Amenity> findCatalogByRoomId(UUID roomId) {
    return catalog.findAllByPhysicalRoomId(roomId).stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public void replaceRoomCatalog(UUID roomId, Set<UUID> ids) {
    roomAmenities.deleteByRoomId(roomId);
    roomAmenities.saveAllAndFlush(
        ids.stream()
            .map(id -> new RoomAmenityJpaEntity(new RoomAmenityJpaEntity.Key(roomId, id)))
            .toList());
  }

  @Override
  public List<CustomAmenity> findCustomByScope(UUID hotelId, UUID roomId) {
    return custom.findByScope(hotelId, roomId).stream()
        .map(CustomAmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<CustomAmenity> findCustomByIdAndScope(UUID id, UUID hotelId, UUID roomId) {
    return custom
        .findByIdAndScope(id, hotelId, roomId)
        .map(CustomAmenityPersistenceMapper::toDomain);
  }

  @Override
  public boolean existsOtherCustomName(UUID hotelId, UUID roomId, String name, UUID excludedId) {
    return custom.existsOtherName(hotelId, roomId, name, excludedId);
  }

  @Override
  public CustomAmenity addCustom(CustomAmenity item) {
    return translateUnique(
        () ->
            CustomAmenityPersistenceMapper.toDomain(
                custom.saveAndFlush(CustomAmenityPersistenceMapper.toNewEntity(item))));
  }

  @Override
  public CustomAmenity updateCustom(CustomAmenity item) {
    var entity =
        custom
            .findByIdAndScope(item.id(), item.hotelId(), item.roomId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.AMENITY_NOT_FOUND));
    if (entity.getVersion() != item.version())
      throw new HotelException(HotelErrorCode.AMENITY_CONCURRENT_UPDATE);
    CustomAmenityPersistenceMapper.applyChanges(entity, item);
    return translateUnique(
        () -> CustomAmenityPersistenceMapper.toDomain(custom.saveAndFlush(entity)));
  }

  private CustomAmenity translateUnique(Supplier<CustomAmenity> operation) {
    try {
      return operation.get();
    } catch (DataIntegrityViolationException error) {
      Throwable cause = error;
      while (cause != null) {
        if (cause instanceof ConstraintViolationException violation
            && ("uq_custom_amenities_hotel_name".equals(violation.getConstraintName())
                || "uq_custom_amenities_room_name".equals(violation.getConstraintName())))
          throw new HotelException(HotelErrorCode.DUPLICATE_CUSTOM_AMENITY);
        cause = cause.getCause();
      }
      throw error;
    }
  }
}

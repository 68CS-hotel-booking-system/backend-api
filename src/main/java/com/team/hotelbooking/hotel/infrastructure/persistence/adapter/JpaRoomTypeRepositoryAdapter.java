package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.RoomTypePersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRoomTypeRepositoryAdapter implements RoomTypeRepository {

  private final RoomTypeJpaRepository roomTypeJpaRepository;

  @Override
  public RoomType add(RoomType roomType) {
    var entity = RoomTypePersistenceMapper.toNewEntity(roomType);
    var saved = roomTypeJpaRepository.saveAndFlush(entity);
    return RoomTypePersistenceMapper.toDomain(saved);
  }

  @Override
  public RoomType update(RoomType roomType) {
    var entity =
        roomTypeJpaRepository
            .findByIdAndHotelId(roomType.id(), roomType.hotelId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ROOM_TYPE_NOT_FOUND));
    RoomTypePersistenceMapper.applyUpdate(entity, roomType);
    return RoomTypePersistenceMapper.toDomain(roomTypeJpaRepository.saveAndFlush(entity));
  }

  @Override
  public Optional<RoomType> findByIdAndHotelId(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository
        .findByIdAndHotelId(roomTypeId, hotelId)
        .map(RoomTypePersistenceMapper::toDomain);
  }

  @Override
  public Optional<RoomType> findByIdAndHotelIdForUpdate(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository
        .findByIdAndHotelIdForUpdate(roomTypeId, hotelId)
        .map(RoomTypePersistenceMapper::toDomain);
  }

  @Override
  public List<RoomType> findActiveByHotelId(UUID hotelId) {
    return roomTypeJpaRepository.findAllByHotelIdAndActiveTrueOrderByNameAscIdAsc(hotelId).stream()
        .map(RoomTypePersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomType> findByHotelId(UUID hotelId) {
    return roomTypeJpaRepository.findAllByHotelIdOrderByNameAscIdAsc(hotelId).stream()
        .map(RoomTypePersistenceMapper::toDomain)
        .toList();
  }
}

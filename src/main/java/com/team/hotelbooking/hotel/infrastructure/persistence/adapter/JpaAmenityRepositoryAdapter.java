package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.AmenityJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelAmenityJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeAmenityJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.AmenityPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.AmenityJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelAmenityJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeAmenityJpaRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaAmenityRepositoryAdapter implements AmenityRepository {

  private final AmenityJpaRepository amenities;
  private final HotelAmenityJpaRepository hotelAmenities;
  private final RoomTypeAmenityJpaRepository roomTypeAmenities;

  @Override
  public List<Amenity> findAll() {
    return amenities.findAllByOrderByCategoryAscNameAscIdAsc().stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Amenity> findByCategory(Amenity.Category category) {
    return amenities
        .findAllByCategoryOrderByNameAscIdAsc(AmenityPersistenceMapper.toDbCategory(category))
        .stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Amenity> findAllByIds(Collection<UUID> amenityIds) {
    if (amenityIds.isEmpty()) {
      return List.of();
    }
    return amenities.findAllById(amenityIds).stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Amenity> findByHotelId(UUID hotelId) {
    return amenities.findAllByHotelId(hotelId).stream()
        .map(AmenityPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Amenity> findByRoomTypeId(UUID roomTypeId) {
    return findByRoomTypeIds(List.of(roomTypeId)).get(roomTypeId);
  }

  @Override
  public Map<UUID, List<Amenity>> findByRoomTypeIds(Collection<UUID> roomTypeIds) {
    Map<UUID, List<Amenity>> result = new LinkedHashMap<>();
    roomTypeIds.forEach(id -> result.put(id, new ArrayList<>()));
    if (roomTypeIds.isEmpty()) {
      return result;
    }

    // Một truy vấn cho cả danh sách loại phòng; giữ thứ tự tên đã sắp trong DB.
    for (Object[] row : amenities.findAllByRoomTypeIds(roomTypeIds)) {
      UUID roomTypeId = (UUID) row[0];
      AmenityJpaEntity amenity = (AmenityJpaEntity) row[1];
      result.get(roomTypeId).add(AmenityPersistenceMapper.toDomain(amenity));
    }

    result.replaceAll((id, list) -> List.copyOf(list));
    return result;
  }

  @Override
  public void replaceHotelAmenities(UUID hotelId, Set<UUID> amenityIds) {
    hotelAmenities.deleteByHotelId(hotelId);
    hotelAmenities.saveAllAndFlush(
        amenityIds.stream()
            .map(
                amenityId ->
                    new HotelAmenityJpaEntity(new HotelAmenityJpaEntity.Key(hotelId, amenityId)))
            .toList());
  }

  @Override
  public void replaceRoomTypeAmenities(UUID roomTypeId, Set<UUID> amenityIds) {
    roomTypeAmenities.deleteByRoomTypeId(roomTypeId);
    roomTypeAmenities.saveAllAndFlush(
        amenityIds.stream()
            .map(
                amenityId ->
                    new RoomTypeAmenityJpaEntity(
                        new RoomTypeAmenityJpaEntity.Key(roomTypeId, amenityId)))
            .toList());
  }
}

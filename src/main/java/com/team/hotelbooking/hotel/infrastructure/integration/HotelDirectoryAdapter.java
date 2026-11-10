package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.hotel.application.usecase.impl.HotelManagementAccess;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.CityJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelSearchJpaQueries;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelDirectoryAdapter implements HotelDirectory {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;
  private final RoomJpaRepository roomJpaRepository;
  private final HotelRepository hotelRepository;
  private final HotelSearchJpaQueries searchQueries;

  @Override
  public Optional<HotelInfo> findById(UUID hotelId) {
    return hotelJpaRepository
        .findById(hotelId)
        .map(
            h ->
                new HotelInfo(
                    h.getId(),
                    h.getOwnerId(),
                    h.getName(),
                    h.getCheckInTime(),
                    h.getCheckOutTime(),
                    h.isActive()));
  }

  @Override
  public boolean existsById(UUID hotelId) {
    return hotelJpaRepository.existsById(hotelId);
  }

  @Override
  public boolean isHotelActive(UUID hotelId) {
    return hotelJpaRepository.findByIdAndActiveTrue(hotelId).isPresent();
  }

  @Override
  public Optional<UUID> findOwnerUserId(UUID hotelId) {
    return hotelJpaRepository.findById(hotelId).map(HotelJpaEntity::getOwnerUserId);
  }

  @Override
  public Optional<HotelSummaryView> findHotelSummary(UUID hotelId) {
    return hotelJpaRepository
        .findByIdAndActiveTrue(hotelId)
        .map(h -> new HotelSummaryView(h.getId(), h.isActive(), false));
  }

  @Override
  public List<RoomTypeSummaryView> findActiveRoomTypes(UUID hotelId) {
    return roomTypeJpaRepository.findAllByHotelIdAndActiveTrue(hotelId).stream()
        .map(
            rt ->
                new RoomTypeSummaryView(
                    rt.getId(),
                    rt.getName(),
                    (int) rt.getMaxOccupancy(),
                    (int) rt.getMaxAdults(),
                    (int) rt.getMaxChildren(),
                    rt.getBasePrice()))
        .toList();
  }

  @Override
  public boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository.findByIdAndHotelIdAndActiveTrue(roomTypeId, hotelId).isPresent();
  }

  // Dùng lại đúng HotelManagementAccess của các API sửa hotel, không chép quy tắc sang module khác.
  @Override
  public Optional<ManagementAccessView> findManagementAccess(
      UUID hotelId, UUID actorUserId, boolean admin) {
    HotelManagementAccess access = new HotelManagementAccess(hotelRepository);
    return hotelRepository
        .findById(hotelId)
        .map(
            hotel ->
                new ManagementAccessView(
                    toView(access.levelOf(actorUserId, admin, hotel)), toView(hotel.status())));
  }

  private static ManagementLevel toView(HotelManagementAccess.Level level) {
    return switch (level) {
      case ADMIN -> ManagementLevel.ADMIN;
      case OWNER -> ManagementLevel.OWNER;
      case MANAGER -> ManagementLevel.MANAGER;
      case RECEPTIONIST -> ManagementLevel.RECEPTIONIST;
      case NONE -> ManagementLevel.NONE;
    };
  }

  private static HotelStatusView toView(Hotel.Status status) {
    return switch (status) {
      case DRAFT -> HotelStatusView.DRAFT;
      case ACTIVE -> HotelStatusView.ACTIVE;
      case SUSPENDED -> HotelStatusView.SUSPENDED;
      case LOCKED -> HotelStatusView.LOCKED;
    };
  }

  @Override
  public Optional<RoomTypeSummaryView> findActiveRoomType(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository
        .findByIdAndHotelIdAndActiveTrue(roomTypeId, hotelId)
        .map(
            rt ->
                new RoomTypeSummaryView(
                    rt.getId(),
                    rt.getName(),
                    (int) rt.getMaxOccupancy(),
                    (int) rt.getMaxAdults(),
                    (int) rt.getMaxChildren(),
                    rt.getBasePrice()));
  }

  @Override
  public long countActiveRoomsByRoomTypeId(UUID roomTypeId) {
    return roomJpaRepository.countByRoomTypeIdAndActiveTrue(roomTypeId);
  }

  @Override
  public List<SearchableHotelView> findActiveHotelsForSearch(HotelSearchFilter filter) {
    List<Object[]> rows =
        searchQueries.findActiveHotelsWithCity(
            filter.cityId(), filter.citySlug(), filter.minStarRating(), filter.amenityIds());
    List<UUID> hotelIds =
        rows.stream().map(row -> HotelSearchJpaQueries.hotelOf(row).getId()).toList();

    Map<UUID, List<AmenityView>> amenitiesByHotel = new HashMap<>();
    for (Object[] row : searchQueries.findHotelAmenities(hotelIds)) {
      amenitiesByHotel
          .computeIfAbsent((UUID) row[0], id -> new ArrayList<>())
          .add(new AmenityView((UUID) row[1], (String) row[2], (String) row[3]));
    }

    return rows.stream()
        .map(
            row -> {
              HotelJpaEntity hotel = HotelSearchJpaQueries.hotelOf(row);
              CityJpaEntity city = HotelSearchJpaQueries.cityOf(row);
              return new SearchableHotelView(
                  hotel.getId(),
                  hotel.getName(),
                  hotel.getSlug(),
                  hotel.getAddress(),
                  hotel.getStarRating(),
                  new CityView(city.getId(), city.getName(), city.getSlug()),
                  amenitiesByHotel.getOrDefault(hotel.getId(), List.of()));
            })
        .toList();
  }

  @Override
  public List<RoomTypeCatalogView> findActiveRoomTypesByHotelIds(Collection<UUID> hotelIds) {
    return searchQueries.findActiveRoomTypesByHotelIds(hotelIds).stream()
        .map(HotelDirectoryAdapter::toCatalogView)
        .toList();
  }

  @Override
  public List<RoomTypeCatalogView> findActiveRoomTypesByIds(Collection<UUID> roomTypeIds) {
    return searchQueries.findActiveRoomTypesOfActiveHotels(roomTypeIds).stream()
        .map(HotelDirectoryAdapter::toCatalogView)
        .toList();
  }

  @Override
  public Map<UUID, Long> countOperationalRoomsByRoomTypeIds(
      Collection<UUID> roomTypeIds, Instant at) {
    return searchQueries.countOperationalRoomsByRoomTypeIds(roomTypeIds, at);
  }

  private static RoomTypeCatalogView toCatalogView(RoomTypeJpaEntity rt) {
    return new RoomTypeCatalogView(
        rt.getId(),
        rt.getHotelId(),
        rt.getName(),
        rt.getMaxOccupancy(),
        rt.getMaxAdults(),
        rt.getMaxChildren(),
        rt.getBasePrice());
  }
}

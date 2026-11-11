package com.team.hotelbooking.pricing.infrastructure.persistence.adapter;

import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateJpaEntity;
import com.team.hotelbooking.pricing.infrastructure.persistence.mapper.RoomRatePersistenceMapper;
import com.team.hotelbooking.pricing.infrastructure.persistence.repository.RoomRateJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRoomRateRepositoryAdapter implements RoomRateRepository {

  private final RoomRateJpaRepository roomRateJpaRepository;
  private final RoomRatePersistenceMapper mapper;

  @Override
  public List<RoomRate> findRatesByRoomTypeIdAndDateRange(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    return roomRateJpaRepository
        .findRatesByRoomTypeIdAndDateRange(roomTypeId, startDate, endDate)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<RoomRate> saveAll(List<RoomRate> rates) {
    List<RoomRateJpaEntity> entities = rates.stream().map(mapper::toEntity).toList();
    return roomRateJpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Map<LocalDate, Long> findDailyAmounts(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    List<RoomRateJpaEntity> rates =
        roomRateJpaRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, startDate, endDate);

    return rates.stream()
        .collect(
            Collectors.toMap(
                r -> r.getId().getRateDate(),
                RoomRateJpaEntity::getAmount,
                (first, second) -> first));
  }
}

package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.HotelPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.HotelStatusHistoryPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelStatusHistoryJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaHotelStatusHistoryRepositoryAdapter implements HotelStatusHistoryRepository {

  private final HotelStatusHistoryJpaRepository historyJpaRepository;

  @Override
  public void add(HotelStatusChange change) {
    historyJpaRepository.saveAndFlush(HotelStatusHistoryPersistenceMapper.toNewEntity(change));
  }

  @Override
  public Optional<HotelStatusChange> findLatestInto(UUID hotelId, Hotel.Status toStatus) {
    return historyJpaRepository
        .findFirstByHotelIdAndToStatusOrderByChangedAtDesc(
            hotelId, HotelPersistenceMapper.toDbStatus(toStatus))
        .map(HotelStatusHistoryPersistenceMapper::toDomain);
  }
}

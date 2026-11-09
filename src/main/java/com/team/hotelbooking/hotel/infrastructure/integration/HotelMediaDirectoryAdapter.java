package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelMediaDirectory;
import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelMediaDirectoryAdapter implements HotelMediaDirectory {

  private final GetManagementHotelUseCase getManagementHotelUseCase;
  private final HotelRepository hotels;
  private final EntityManager entityManager;

  @Override
  public void requireManage(UUID actorId, UUID hotelId) {
    getManagementHotelUseCase.execute(new GetManagementHotelQuery(actorId, hotelId));
  }

  @Override
  public void lockForImageUpdate(UUID actorId, UUID hotelId, long expectedVersion) {

    HotelJpaEntity hotel =
        entityManager.find(HotelJpaEntity.class, hotelId, LockModeType.PESSIMISTIC_WRITE);

    if (hotel == null) {
      throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    }

    requireManage(actorId, hotelId);

    if (hotel.getVersion() == null || hotel.getVersion().longValue() != expectedVersion) {
      throw new OptimisticLockException("Phiên bản khách sạn đã thay đổi.");
    }

    entityManager.lock(hotel, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
  }

  @Override
  public boolean isPublished(UUID hotelId) {
    return hotels
        .findById(hotelId)
        .map(hotel -> hotel.status() == Hotel.Status.ACTIVE)
        .orElse(false);
  }
}

package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelMediaDirectory;
import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.HotelManagementAccess;
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
  private final IdentityAccounts accounts;

  @Override
  public void requireManage(UUID actorId, UUID hotelId) {
    getManagementHotelUseCase.execute(new GetManagementHotelQuery(actorId, hotelId));
  }

  @Override
  public void requireImageUpload(UUID actorId, UUID hotelId) {
    requireManage(actorId, hotelId);
    if (accounts.findById(actorId).orElseThrow().roles().contains("ADMIN")) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    if (hotels.hasPendingReview(hotelId)) {
      throw new HotelException(HotelErrorCode.REVIEW_PENDING);
    }
    // Upload mới chưa gắn vào hồ sơ; PUT images mới là bước ghi và luôn khóa hotel.
  }

  @Override
  public void lockForImageUpdate(UUID actorId, UUID hotelId, long expectedVersion) {

    HotelJpaEntity hotel =
        entityManager.find(HotelJpaEntity.class, hotelId, LockModeType.PESSIMISTIC_WRITE);

    if (hotel == null) {
      throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    }

    var actor =
        accounts
            .findById(actorId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));
    if (!actor.active() || !actor.emailVerified()) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    new HotelManagementAccess(hotels)
        .requireEditableHotel(actorId, actor.roles().contains("ADMIN"), hotelId);

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

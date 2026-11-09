package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.time.Clock;
import java.util.UUID;

public class ApproveHotelService implements ApproveHotelUseCase {

  private final HotelRepository hotels;
  private final IdentityAccounts accounts;
  private final Clock clock;

  public ApproveHotelService(HotelRepository hotels, IdentityAccounts accounts, Clock clock) {
    this.hotels = hotels;
    this.accounts = accounts;
    this.clock = clock;
  }

  @Override
  public ManagementHotelResult execute(UUID hotelId, UUID adminActorId) {
    if (hotelId == null || adminActorId == null) {
      throw new HotelException(HotelErrorCode.VALIDATION_FAILED);
    }

    // 1. Kiểm tra tài khoản admin
    AccountSnapshot admin =
        accounts
            .findById(adminActorId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));

    if (!admin.active() || !admin.emailVerified() || !admin.roles().contains("ADMIN")) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    // 2. Tìm khách sạn
    Hotel hotel =
        hotels
            .findById(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    // 3. Domain Logic: chuyển trạng thái sang ACTIVE (DRAFT -> ACTIVE)
    Hotel activatedHotel = hotel.activate(clock.instant());

    // 4. Lưu lại vào DB
    Hotel savedHotel = hotels.save(activatedHotel);

    return ManagementHotelResult.from(savedHotel);
  }
}

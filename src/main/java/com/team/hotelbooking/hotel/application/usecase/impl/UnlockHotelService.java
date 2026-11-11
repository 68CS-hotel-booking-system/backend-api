package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.UnlockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.UnlockHotelUseCase;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.util.Objects;

// HT-09: chỉ ADMIN gỡ khóa; lý do bắt buộc. Gỡ khóa về SUSPENDED (khóa từ DRAFT thì về DRAFT),
// không tự mở bán lại.
public class UnlockHotelService implements UnlockHotelUseCase {

  private final HotelSalesLifecycle lifecycle;
  private final Clock clock;

  public UnlockHotelService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository history,
      Clock clock) {
    this.lifecycle = new HotelSalesLifecycle(hotels, roomTypes, rooms, history);
    this.clock = clock;
  }

  @Override
  public ManagementHotelResult execute(UnlockHotelCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    Hotel hotel = lifecycle.lockForStatusChange(command.hotelId());
    Hotel unlocked = hotel.unlock(lifecycle.statusBeforeLatestLock(hotel.id()), clock.instant());

    return lifecycle.record(hotel, unlocked, command.reason(), true, command.actorUserId());
  }
}

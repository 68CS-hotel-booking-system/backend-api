package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.LockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.LockHotelUseCase;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.util.Objects;

// HT-09: ADMIN khóa bán từ mọi trạng thái chưa khóa; lý do bắt buộc.
public class LockHotelService implements LockHotelUseCase {

  private final HotelSalesLifecycle lifecycle;
  private final Clock clock;

  public LockHotelService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository history,
      Clock clock) {
    this.lifecycle = new HotelSalesLifecycle(hotels, roomTypes, rooms, history);
    this.clock = clock;
  }

  @Override
  public ManagementHotelResult execute(LockHotelCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    Hotel hotel = lifecycle.lockForStatusChange(command.hotelId());
    Hotel locked = hotel.lock(clock.instant());

    return lifecycle.record(hotel, locked, command.reason(), true, command.actorUserId());
  }
}

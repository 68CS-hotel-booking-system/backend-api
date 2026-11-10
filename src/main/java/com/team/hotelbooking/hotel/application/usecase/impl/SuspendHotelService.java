package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.SuspendHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.SuspendHotelUseCase;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.util.Objects;

// HT-05/HT-06/HT-09: quản lý tạm ngừng bán; chỉ chặn đơn mới, không động tới đơn đã có.
public class SuspendHotelService implements SuspendHotelUseCase {

  private final HotelManagementAccess access;

  private final HotelSalesLifecycle lifecycle;
  private final Clock clock;

  public SuspendHotelService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository history,
      Clock clock) {
    this.access = new HotelManagementAccess(hotels);
    this.lifecycle = new HotelSalesLifecycle(hotels, roomTypes, rooms, history);
    this.clock = clock;
  }

  @Override
  public ManagementHotelResult execute(SuspendHotelCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    // Quyền giống PATCH khách sạn: RECEPTIONIST → 403, người ngoài/hotel không tồn tại → 404.
    access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());
    Hotel hotel = lifecycle.lockForStatusChange(command.hotelId());
    Hotel suspended = hotel.suspend(clock.instant());

    return lifecycle.record(hotel, suspended, command.reason(), false, command.actorUserId());
  }
}

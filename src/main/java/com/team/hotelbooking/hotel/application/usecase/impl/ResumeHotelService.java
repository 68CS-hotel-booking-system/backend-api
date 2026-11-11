package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ResumeHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ResumeHotelUseCase;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

// HT-09: quản lý mở bán lại khách sạn đang SUSPENDED; không gỡ LOCKED, không duyệt lần
// đầu. Giả định chưa chốt: quản lý được tự mở lại sau khi tạm ngừng; vẫn kiểm lại HT-03.
public class ResumeHotelService implements ResumeHotelUseCase {

  private final HotelManagementAccess access;

  private final HotelSalesLifecycle lifecycle;
  private final Clock clock;

  public ResumeHotelService(
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
  public ManagementHotelResult execute(ResumeHotelCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());
    Hotel hotel = lifecycle.lockForStatusChange(command.hotelId());
    Instant now = clock.instant();
    Hotel resumed = hotel.resume(now);
    lifecycle.requireReadyToSell(resumed, now);

    return lifecycle.record(hotel, resumed, command.reason(), false, command.actorUserId());
  }
}

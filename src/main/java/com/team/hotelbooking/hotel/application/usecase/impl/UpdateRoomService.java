package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.UpdateRoomCommand;
import com.team.hotelbooking.hotel.application.port.RoomInventoryPort;
import com.team.hotelbooking.hotel.application.result.RoomResult;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class UpdateRoomService implements UpdateRoomUseCase {

  private final HotelManagementAccess access;
  private final RoomRepository rooms;
  private final Clock clock;
  private final RoomInventoryPort inventory;

  public UpdateRoomService(
      HotelRepository hotels, RoomRepository rooms, Clock clock, RoomInventoryPort inventory) {
    this.access = new HotelManagementAccess(hotels);
    this.rooms = rooms;
    this.clock = clock;
    this.inventory = inventory;
  }

  @Override
  public RoomResult execute(UpdateRoomCommand command) {
    Objects.requireNonNull(command, "Command không được null.");

    // Kiểm quyền theo hotel trước, rồi mới xét bản ghi con có thuộc hotel hay không.
    access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());

    Room room =
        rooms
            .findByIdAndHotelId(command.roomId(), command.hotelId())
            .orElseThrow(
                () ->
                    HotelException.roomNotFound(
                        "Không tìm thấy phòng vật lý hoặc phòng không thuộc khách sạn."));

    if (command.active() != null) {
      // Cùng khóa room_type với hold/gán phòng; đọc lại sau khi chờ khóa.
      inventory.lockRoomType(command.hotelId(), room.roomTypeId());
      room =
          rooms
              .findByIdAndHotelId(command.roomId(), command.hotelId())
              .orElseThrow(() -> HotelException.roomNotFound("Không tìm thấy phòng vật lý."));
    }
    String roomNumber = changedRoomNumber(command, room);
    String floor = command.floor() == null ? null : String.valueOf(command.floor());
    Room.Readiness readiness = command.status() == null ? null : readinessOf(command.status());

    Instant now = clock.instant();
    if (Boolean.FALSE.equals(command.active()) && !now.isAfter(room.operationalFrom())) {
      throw HotelException.invalidRequest("Phòng chưa đến thời điểm bắt đầu phục vụ.");
    }
    Room updated = room.update(roomNumber, floor, readiness, command.active(), now);
    Room saved = rooms.update(updated);
    if (Boolean.FALSE.equals(command.active())) {
      // Repository flush trước khi kiểm tra; lỗi sẽ rollback cả cập nhật phòng.
      inventory.requireAllocatableAfterDeactivation(room.roomTypeId(), room.id());
    }
    return RoomResult.from(saved, now);
  }

  // Chỉ đổi khi khác số hiện tại (không phân biệt hoa thường); trùng phòng khác cùng hotel → lỗi.
  private String changedRoomNumber(UpdateRoomCommand command, Room room) {
    if (command.roomNumber() == null) {
      return null;
    }

    String trimmed = command.roomNumber().trim();
    if (trimmed.isEmpty()) {
      throw HotelException.invalidRequest("Số phòng không được để trống.");
    }
    if (trimmed.equalsIgnoreCase(room.roomNumber())) {
      return null;
    }
    if (rooms.existsOtherRoomWithNumber(command.hotelId(), trimmed, command.roomId())) {
      throw HotelException.duplicateRoomNumber(
          "Số phòng '" + trimmed + "' đã tồn tại trong khách sạn này.");
    }
    return trimmed;
  }

  // Hợp đồng HT21 nhận ready/cleaning/maintenance, nhưng cột readiness chỉ có READY/DIRTY nên
  // cleaning và maintenance đều lưu DIRTY.
  private static Room.Readiness readinessOf(String status) {
    return switch (status.trim().toLowerCase(Locale.ROOT)) {
      case "ready" -> Room.Readiness.READY;
      case "cleaning", "maintenance" -> Room.Readiness.DIRTY;
      default ->
          throw HotelException.invalidRequest(
              "Trạng thái buồng phòng không hợp lệ. Chỉ chấp nhận: ready, cleaning, maintenance.");
    };
  }
}

package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.UpdateHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.UpdateHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.time.Clock;

public class UpdateHotelService implements UpdateHotelUseCase {

  private final HotelRepository hotels;
  private final HotelManagementAccess access;
  private final Clock clock;

  public UpdateHotelService(HotelRepository hotels, Clock clock) {
    this.hotels = hotels;
    this.access = new HotelManagementAccess(hotels);
    this.clock = clock;
  }

  @Override
  public ManagementHotelResult execute(UpdateHotelCommand command) {
    if (command == null || command.actorUserId() == null || command.hotelId() == null) {
      throw HotelException.invalid("Thông tin người thực hiện và khách sạn là bắt buộc.");
    }
    if (command.version() == null) {
      throw HotelException.invalid("Version của khách sạn là bắt buộc.");
    }

    // Không tra lại trạng thái tài khoản ở đây: principal do CurrentUserJwtConverter dựng đã chặn
    // tài khoản BANNED/INACTIVE/PENDING, kiểm auth_version và nạp role từ DB. Service chỉ kiểm quan
    // hệ ADMIN/Owner/HOTEL_MANAGER với đúng hotel (xem HotelManagementAccess).
    Hotel hotel =
        access.requireEditableHotel(command.actorUserId(), command.admin(), command.hotelId());

    Hotel updated =
        hotel.updateDetails(
            command.name(),
            command.description(),
            command.address(),
            command.contactPhone(),
            command.contactEmail(),
            command.latitude(),
            command.longitude(),
            command.starRating(),
            command.checkInTime(),
            command.checkOutTime(),
            clock.instant());

    // HT-07: chỉ ghi bảng hotels. Đơn đặt phải giữ bản chụp tên/chính sách của riêng nó,
    // nên thay đổi ở đây không sửa dữ liệu đơn đã ghi nhận.
    return ManagementHotelResult.from(hotels.update(updated, command.version()));
  }
}

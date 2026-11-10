package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

// HT-03/HT-09: chỉ ADMIN duyệt công khai lần đầu (DRAFT → ACTIVE), khi đủ điều kiện mở bán.
public class ApproveHotelService implements ApproveHotelUseCase {

  private final IdentityAccounts accounts;
  private final HotelSalesLifecycle lifecycle;
  private final Clock clock;

  public ApproveHotelService(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository history,
      IdentityAccounts accounts,
      Clock clock) {
    this.accounts = accounts;
    this.lifecycle = new HotelSalesLifecycle(hotels, roomTypes, rooms, history);
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

    // 2. Khóa hàng hotel để duyệt không chạy song song với khóa/sửa trạng thái khác.
    Hotel hotel = lifecycle.lockForStatusChange(hotelId);

    // 3. Kiểm trạng thái (DRAFT) trước HT-03 để hotel không còn DRAFT nhận lỗi trạng thái, không
    // nhận 422; rồi kiểm HT-03 bằng cùng hàm với resume.
    Instant now = clock.instant();
    Hotel activatedHotel = hotel.activate(now);
    lifecycle.requireReadyToSell(activatedHotel, now);

    // 4. Lưu trạng thái và lịch sử trong cùng transaction.
    return lifecycle.record(hotel, activatedHotel, null, false, adminActorId);
  }
}

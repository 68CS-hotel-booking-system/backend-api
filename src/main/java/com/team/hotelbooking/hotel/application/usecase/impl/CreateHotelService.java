package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.CreateHotelCommand;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.CreateHotelResult;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class CreateHotelService implements CreateHotelUseCase {

  private static final int MAX_SLUG_LENGTH = 255;

  private final HotelRepository hotels;
  private final CityRepository cities;
  private final IdentityAccounts accounts;
  private final Clock clock;

  public CreateHotelService(
      HotelRepository hotels, CityRepository cities, IdentityAccounts accounts, Clock clock) {

    this.hotels = hotels;
    this.cities = cities;
    this.accounts = accounts;
    this.clock = clock;
  }

  @Override
  public CreateHotelResult execute(CreateHotelCommand command) {
    if (command == null) {
      throw new HotelException(HotelErrorCode.VALIDATION_FAILED);
    }

    // 1. Chỉ kiểm tra actorId và cityId bắt buộc (ownerId có thể null nếu Owner tự tạo)
    if (command.actorId() == null || command.cityId() == null) {
      throw new HotelException(HotelErrorCode.VALIDATION_FAILED);
    }

    AccountSnapshot actor =
        accounts
            .findById(command.actorId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));

    // kiểm tra actor phải active và đã xác thực email
    if (!actor.active() || !actor.emailVerified()) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    // 2. Xác định targetOwnerId dựa trên quyền và dữ liệu truyền lên
    UUID targetOwnerId;
    if (command.ownerId() != null && !command.ownerId().equals(actor.id())) {
      // Muốn chỉ định owner khác thì BẮT BUỘC actor phải có quyền ADMIN
      if (!actor.roles().contains("ADMIN")) {
        throw new HotelException(HotelErrorCode.ACCESS_DENIED);
      }
      targetOwnerId = command.ownerId();
    } else {
      // Không truyền ownerId thì actor phải là HOTEL_OWNER tự tạo cho chính mình
      if (!actor.roles().contains("HOTEL_OWNER")) {
        throw new HotelException(HotelErrorCode.ACCESS_DENIED);
      }
      targetOwnerId = actor.id();
    }

    // 3. Lấy thông tin Owner và kiểm tra tính hợp lệ
    AccountSnapshot owner =
        targetOwnerId.equals(actor.id())
            ? actor
            : accounts
                .findById(targetOwnerId)
                .orElseThrow(() -> new HotelException(HotelErrorCode.OWNER_NOT_FOUND));

    if (!owner.active() || !owner.emailVerified() || !owner.roles().contains("HOTEL_OWNER")) {
      throw new HotelException(HotelErrorCode.OWNER_NOT_ELIGIBLE);
    }

    // 4. Kiểm tra thành phố
    if (!cities.existsById(command.cityId())) {
      throw new HotelException(HotelErrorCode.CITY_NOT_FOUND);
    }

    // 5. Khởi tạo Draft & Lưu DB
    UUID hotelId = UUID.randomUUID();
    Instant now = clock.instant();
    String slug = generateSlug(command.name(), hotelId);

    Hotel hotel =
        Hotel.createDraft(
            hotelId,
            targetOwnerId,
            command.cityId(),
            command.name(),
            slug,
            command.description(),
            command.address(),
            command.latitude(),
            command.longitude(),
            command.starRating(),
            command.checkInTime(),
            command.checkOutTime(),
            now);

    Hotel savedHotel = hotels.add(hotel);

    return CreateHotelResult.from(savedHotel);
  }

  private static String generateSlug(String name, UUID hotelId) {
    String base =
        Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replace('đ', 'd')
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");

    if (base.isBlank()) {
      base = "hotel";
    }

    String suffix = hotelId.toString();
    int maxBaseLength = MAX_SLUG_LENGTH - suffix.length() - 1;

    if (base.length() > maxBaseLength) {
      base = base.substring(0, maxBaseLength).replaceAll("-+$", "");
    }

    return base + "-" + suffix;
  }
}

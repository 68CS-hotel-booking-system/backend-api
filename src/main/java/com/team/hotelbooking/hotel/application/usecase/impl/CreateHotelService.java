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

    if (command.actorId() == null || command.ownerId() == null || command.cityId() == null) {
      throw new HotelException(HotelErrorCode.VALIDATION_FAILED);
    }

    AccountSnapshot actor =
        accounts
            .findById(command.actorId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));

    if (!actor.active() || !actor.emailVerified() || !actor.roles().contains("ADMIN")) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    AccountSnapshot owner =
        accounts
            .findById(command.ownerId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.OWNER_NOT_FOUND));

    if (!owner.active() || !owner.emailVerified() || !owner.roles().contains("HOTEL_OWNER")) {
      throw new HotelException(HotelErrorCode.OWNER_NOT_ELIGIBLE);
    }

    if (!cities.existsById(command.cityId())) {
      throw new HotelException(HotelErrorCode.CITY_NOT_FOUND);
    }

    UUID hotelId = UUID.randomUUID();
    Instant now = clock.instant();
    String slug = generateSlug(command.name(), hotelId);

    Hotel hotel =
        Hotel.createDraft(
            hotelId,
            command.ownerId(),
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

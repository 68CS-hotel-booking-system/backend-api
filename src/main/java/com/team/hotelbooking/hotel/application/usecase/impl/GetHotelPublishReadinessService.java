package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.GetHotelPublishReadinessQuery;
import com.team.hotelbooking.hotel.application.result.HotelPublishReadinessResult;
import com.team.hotelbooking.hotel.application.usecase.GetHotelPublishReadinessUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.hotel.domain.service.HotelPublishReadiness;
import java.time.Clock;

public final class GetHotelPublishReadinessService implements GetHotelPublishReadinessUseCase {
  private final HotelManagementAccess access;
  private final RoomRepository rooms;
  private final RoomTypeRepository types;
  private final Clock clock;

  public GetHotelPublishReadinessService(
      HotelRepository hotels, RoomRepository rooms, RoomTypeRepository types, Clock clock) {
    this.access = new HotelManagementAccess(hotels);
    this.rooms = rooms;
    this.types = types;
    this.clock = clock;
  }

  @Override
  public HotelPublishReadinessResult execute(GetHotelPublishReadinessQuery query) {
    if (query == null || query.actorId() == null || query.hotelId() == null)
      throw HotelException.invalid("Người thực hiện và khách sạn là bắt buộc.");
    var hotel = access.requireManageableHotel(query.actorId(), query.admin(), query.hotelId());
    var at = clock.instant();
    var counts = rooms.countOperationalRoomsByRoomType(hotel.id(), at);
    var unmet =
        HotelPublishReadiness.unmetRequirements(hotel, types.findByHotelId(hotel.id()), counts);
    return new HotelPublishReadinessResult(
        hotel.id(),
        unmet.isEmpty(),
        counts.values().stream().mapToLong(Long::longValue).sum(),
        at,
        unmet.stream()
            .map(item -> new HotelPublishReadinessResult.Requirement(item.field(), item.message()))
            .toList());
  }
}

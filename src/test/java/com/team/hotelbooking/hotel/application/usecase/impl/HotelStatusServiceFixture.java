package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.RoomType;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mock;

// Fixture chung cho test các service đổi trạng thái bán; mỗi lớp con tự tạo service trong setUp.
abstract class HotelStatusServiceFixture {

  static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  static final UUID OWNER_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  static final UUID ADMIN_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  static final UUID ROOM_TYPE_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b");
  static final Instant CREATED = Instant.parse("2026-11-01T00:00:00Z");
  static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

  @Mock HotelRepository hotels;
  @Mock RoomTypeRepository roomTypes;
  @Mock RoomRepository rooms;
  @Mock HotelStatusHistoryRepository history;

  final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  static Hotel hotel(Hotel.Status status, String contactPhone) {
    return new Hotel(
        HOTEL_ID,
        OWNER_ID,
        UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3"),
        "Khách sạn Biển Xanh",
        "khach-san-bien-xanh",
        null,
        "12 Võ Nguyên Giáp",
        contactPhone,
        null,
        null,
        null,
        null,
        LocalTime.of(14, 0),
        LocalTime.of(12, 0),
        status,
        CREATED,
        CREATED,
        2L);
  }

  static Hotel hotel(Hotel.Status status) {
    return hotel(status, "+84905123456");
  }

  static RoomType sellableRoomType() {
    return new RoomType(
        ROOM_TYPE_ID,
        HOTEL_ID,
        "Deluxe",
        null,
        (short) 2,
        (short) 1,
        (short) 3,
        0L,
        true,
        CREATED,
        CREATED,
        0L);
  }

  static HotelErrorCode codeOf(Throwable exception) {
    return ((HotelException) exception).code();
  }

  void givenLockedRow(Hotel hotel) {
    given(hotels.findByIdForUpdate(HOTEL_ID)).willReturn(Optional.of(hotel));
  }

  void givenStatusUpdateReturnsSavedHotel() {
    given(hotels.updateStatus(any(Hotel.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  void givenSellableInventory(long operationalRooms) {
    given(roomTypes.findByHotelId(HOTEL_ID)).willReturn(List.of(sellableRoomType()));
    given(rooms.countOperationalRoomsByRoomType(HOTEL_ID, NOW))
        .willReturn(operationalRooms == 0 ? Map.of() : Map.of(ROOM_TYPE_ID, operationalRooms));
  }
}

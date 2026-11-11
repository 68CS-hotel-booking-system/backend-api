package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.CreateCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.command.DetachCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.command.GetManagementAmenitiesQuery;
import com.team.hotelbooking.hotel.application.command.ReplaceRoomAmenitiesCommand;
import com.team.hotelbooking.hotel.application.command.UpdateCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.result.CustomAmenityResult;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.result.ManagementAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.CreateCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.DetachCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetManagementAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateCustomAmenityUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.model.CustomAmenity;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.ScopedAmenityRepository;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ScopedAmenityService
    implements GetManagementAmenitiesUseCase,
        CreateCustomAmenityUseCase,
        UpdateCustomAmenityUseCase,
        DetachCustomAmenityUseCase,
        ReplaceRoomAmenitiesUseCase {
  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final AmenityRepository amenities;
  private final ScopedAmenityRepository scoped;
  private final HotelManagementAccess access;
  private final Clock clock;

  public ScopedAmenityService(
      HotelRepository hotels,
      RoomRepository rooms,
      AmenityRepository amenities,
      ScopedAmenityRepository scoped,
      Clock clock) {
    this.hotels = hotels;
    this.rooms = rooms;
    this.amenities = amenities;
    this.scoped = scoped;
    this.access = new HotelManagementAccess(hotels);
    this.clock = clock;
  }

  @Override
  public ManagementAmenitiesResult execute(GetManagementAmenitiesQuery query) {
    if (query == null) throw HotelException.invalid("Thông tin truy vấn là bắt buộc.");
    Room room =
        requireScope(query.actorUserId(), query.admin(), query.hotelId(), query.roomId(), false);
    return list(query.hotelId(), query.roomId(), room);
  }

  @Override
  public CustomAmenityResult execute(CreateCustomAmenityCommand command) {
    if (command == null) throw HotelException.invalid("Thông tin tiện ích là bắt buộc.");
    requireScope(command.actorUserId(), command.admin(), command.hotelId(), command.roomId(), true);
    CustomAmenity item =
        CustomAmenity.create(
            UUID.randomUUID(),
            command.hotelId(),
            command.roomId(),
            command.name(),
            command.description(),
            clock.instant());
    requireUniqueName(item);
    return CustomAmenityResult.from(scoped.addCustom(item));
  }

  @Override
  public CustomAmenityResult execute(UpdateCustomAmenityCommand command) {
    if (command == null) throw HotelException.invalid("Thông tin cập nhật là bắt buộc.");
    requireScope(command.actorUserId(), command.admin(), command.hotelId(), command.roomId(), true);
    CustomAmenity current =
        requireCustom(
            command.amenityId(), command.hotelId(), command.roomId(), command.expectedVersion());
    CustomAmenity updated =
        current.update(command.name(), command.description(), command.attached(), clock.instant());
    requireUniqueName(updated);
    return CustomAmenityResult.from(scoped.updateCustom(updated));
  }

  @Override
  public CustomAmenityResult execute(DetachCustomAmenityCommand command) {
    if (command == null) throw HotelException.invalid("Thông tin tháo tiện ích là bắt buộc.");
    requireScope(command.actorUserId(), command.admin(), command.hotelId(), command.roomId(), true);
    CustomAmenity current =
        requireCustom(
            command.amenityId(), command.hotelId(), command.roomId(), command.expectedVersion());
    // Tháo khỏi đối tượng nhưng giữ bản ghi riêng để có thể gắn lại, không xóa danh mục chung.
    if (!current.attached()) return CustomAmenityResult.from(current);
    return CustomAmenityResult.from(
        scoped.updateCustom(current.update(null, null, false, clock.instant())));
  }

  @Override
  public ManagementAmenitiesResult execute(ReplaceRoomAmenitiesCommand command) {
    if (command == null || command.roomId() == null)
      throw HotelException.invalid("Phòng vật lý là bắt buộc.");
    Set<UUID> ids = ReplaceHotelAmenitiesService.requireAmenityIds(command.amenityIds());
    Room room =
        requireScope(
            command.actorUserId(), command.admin(), command.hotelId(), command.roomId(), true);
    Amenity.requireAssignable(ids, amenities.findAllByIds(ids), Amenity.Category.ROOM);
    scoped.replaceRoomCatalog(command.roomId(), ids);
    return list(command.hotelId(), command.roomId(), room);
  }

  private Room requireScope(UUID actorId, boolean admin, UUID hotelId, UUID roomId, boolean write) {
    if (actorId == null || hotelId == null)
      throw HotelException.invalid("Người thực hiện và khách sạn là bắt buộc.");
    access.requireEditableHotel(actorId, admin, hotelId);
    if (write) {
      // Cùng thứ tự khóa với PUT hotel amenities; tuần tự hóa toàn bộ ghi tiện ích trong hotel.
      hotels
          .findByIdForUpdate(hotelId)
          .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
      access.requireEditableHotel(actorId, admin, hotelId);
    }
    if (roomId == null) return null;
    return rooms
        .findByIdAndHotelId(roomId, hotelId)
        .orElseThrow(() -> new HotelException(HotelErrorCode.ROOM_NOT_FOUND));
  }

  private CustomAmenity requireCustom(UUID id, UUID hotelId, UUID roomId, Long expectedVersion) {
    if (id == null || expectedVersion == null || expectedVersion < 0)
      throw HotelException.invalid("ID tiện ích và phiên bản hợp lệ là bắt buộc.");
    CustomAmenity current =
        scoped
            .findCustomByIdAndScope(id, hotelId, roomId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.AMENITY_NOT_FOUND));
    if (current.version() != expectedVersion)
      throw new HotelException(HotelErrorCode.AMENITY_CONCURRENT_UPDATE);
    return current;
  }

  private void requireUniqueName(CustomAmenity item) {
    if (scoped.existsOtherCustomName(
        item.hotelId(), item.roomId(), item.normalizedName(), item.id()))
      throw new HotelException(HotelErrorCode.DUPLICATE_CUSTOM_AMENITY);
  }

  private ManagementAmenitiesResult list(UUID hotelId, UUID roomId, Room room) {
    List<Amenity> selected =
        roomId == null ? amenities.findByHotelId(hotelId) : scoped.findCatalogByRoomId(roomId);
    List<ListAmenitiesResult> inherited =
        room == null
            ? List.of()
            : amenities.findByRoomTypeId(room.roomTypeId()).stream()
                .map(ListAmenitiesResult::from)
                .toList();
    return new ManagementAmenitiesResult(
        hotelId,
        roomId,
        selected.stream().map(ListAmenitiesResult::from).toList(),
        scoped.findCustomByScope(hotelId, roomId).stream().map(CustomAmenityResult::from).toList(),
        inherited);
  }
}

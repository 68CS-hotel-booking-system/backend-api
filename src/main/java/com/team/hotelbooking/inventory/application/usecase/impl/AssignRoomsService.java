package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand;
import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand.RoomAssignmentItem;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.PhysicalRoomSummary;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.usecase.AssignRoomsUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.PhysicalRoom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AssignRoomsService implements AssignRoomsUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final BookingStayPort bookingStayPort;
  private final RoomAssignmentRepository roomAssignmentRepository;
  private final RoomBlockRepository roomBlockRepository;
  private final InventoryQueryPort inventoryQueryPort;
  private final Clock clock;

  public AssignRoomsService(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      RoomBlockRepository roomBlockRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.bookingStayPort = bookingStayPort;
    this.roomAssignmentRepository = roomAssignmentRepository;
    this.roomBlockRepository = roomBlockRepository;
    this.inventoryQueryPort = inventoryQueryPort;
    this.clock = clock;
  }

  @Override
  public BookingRoomAssignmentsResult execute(AssignRoomsCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.bookingId() == null
        || command.assignments() == null
        || command.assignments().isEmpty()) {
      throw InventoryException.invalidStayOperation(
          "Thông tin đơn đặt phòng và danh sách phòng vật lý cần phân không được để trống.");
    }

    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    BookingStayInfo booking =
        bookingStayPort
            .findBookingForStayOperation(command.hotelId(), command.bookingId())
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng đã xác nhận trong khách sạn."));

    if (booking.status() != StayStatus.CONFIRMED) {
      throw InventoryException.invalidStayOperation(
          "Chỉ được phân hoặc đổi phòng vật lý cho đơn đặt phòng ở trạng thái CONFIRMED (BK-15).");
    }

    List<UUID> roomTypeIds =
        booking.items().stream().map(BookingItemStayInfo::roomTypeId).distinct().sorted().toList();
    hotelCatalogPort.lockRoomTypesInHotel(command.hotelId(), roomTypeIds);

    Instant now = clock.instant();

    Map<UUID, BookingItemStayInfo> itemsById =
        booking.items().stream()
            .collect(Collectors.toMap(BookingItemStayInfo::bookingItemId, i -> i));

    if (command.assignments().size() != itemsById.size()) {
      throw InventoryException.invalidStayOperation(
          "Phải cung cấp phân công phòng cho đầy đủ mọi hạng mục trong đơn đặt phòng (BK-15).");
    }

    List<PhysicalRoomSummary> hotelRooms =
        hotelCatalogPort.findPhysicalRoomsByHotelId(command.hotelId());
    Map<UUID, PhysicalRoomSummary> roomsById =
        hotelRooms.stream().collect(Collectors.toMap(PhysicalRoomSummary::id, r -> r));

    Set<UUID> seenRoomIdsInRequest = new HashSet<>();
    Set<UUID> seenItemIdsInRequest = new HashSet<>();

    for (RoomAssignmentItem reqItem : command.assignments()) {
      if (reqItem == null
          || reqItem.bookingItemId() == null
          || reqItem.roomIds() == null
          || reqItem.roomIds().isEmpty()) {
        throw InventoryException.invalidStayOperation(
            "Hạng mục phân công phòng không được để trống.");
      }
      if (!seenItemIdsInRequest.add(reqItem.bookingItemId())) {
        throw InventoryException.invalidStayOperation(
            "Hạng mục đặt phòng bị lặp trong yêu cầu phân phòng.");
      }

      BookingItemStayInfo bookingItem = itemsById.get(reqItem.bookingItemId());
      if (bookingItem == null) {
        throw InventoryException.invalidStayOperation(
            "Hạng mục đặt phòng không thuộc đơn đặt phòng này.");
      }

      if (reqItem.roomIds().size() != bookingItem.quantity()) {
        throw InventoryException.invalidStayOperation(
            "Số lượng phòng vật lý được gán phải khớp chính xác với số lượng phòng đã đặt của hạng mục (BK-15).");
      }

      LocalDate effectiveStartDate = bookingItem.checkInDate();

      for (UUID roomId : reqItem.roomIds()) {
        if (roomId == null || !seenRoomIdsInRequest.add(roomId)) {
          throw InventoryException.roomAssignmentConflict(
              "Không thể gán trùng một phòng vật lý cho nhiều suất trong cùng đơn đặt phòng (BK-15).");
        }

        PhysicalRoomSummary room = roomsById.get(roomId);
        if (room == null) {
          throw InventoryException.roomNotFound(
              "Phòng vật lý không tồn tại hoặc không thuộc khách sạn này.");
        }

        if (!bookingItem.roomTypeId().equals(room.roomTypeId())) {
          throw InventoryException.roomAssignmentConflict(
              "Phòng vật lý "
                  + room.roomNumber()
                  + " không thuộc đúng loại phòng của hạng mục đặt phòng (BK-15).");
        }

        PhysicalRoom probe =
            new PhysicalRoom(room.id(), room.operationalFrom(), room.operationalUntil(), Set.of());
        if (!probe.isAvailableForRange(effectiveStartDate, bookingItem.checkOutDate(), now)) {
          throw InventoryException.roomAssignmentConflict(
              "Phòng vật lý "
                  + room.roomNumber()
                  + " không còn hiệu lực phục vụ trong toàn bộ kỳ lưu trú còn lại (BK-15).");
        }

        List<RoomBlock> overlappingBlocks =
            roomBlockRepository.findActiveBlocksByRoomIdInRange(
                room.roomTypeId(),
                room.id(),
                effectiveStartDate,
                bookingItem.checkOutDate().minusDays(1));
        if (!overlappingBlocks.isEmpty()) {
          throw InventoryException.roomAssignmentConflict(
              "Phòng vật lý "
                  + room.roomNumber()
                  + " đang bị chặn trong khoảng ngày lưu trú (BK-15).");
        }

        List<RoomAssignment> overlappingAssignments =
            roomAssignmentRepository
                .findActiveByRoomIdOverlapping(
                    room.id(), effectiveStartDate, bookingItem.checkOutDate())
                .stream()
                .filter(a -> !command.bookingId().equals(a.bookingId()))
                .toList();
        if (!overlappingAssignments.isEmpty()) {
          throw InventoryException.roomAssignmentConflict(
              "Phòng vật lý "
                  + room.roomNumber()
                  + " đã được phân cho một đơn đặt phòng khác chồng lịch (BK-15).");
        }
      }
    }

    List<RoomAssignment> currentActive =
        roomAssignmentRepository.findActiveByBookingId(command.bookingId());

    if (!currentActive.isEmpty()
        && (command.reason() == null || command.reason().trim().isEmpty())) {
      throw InventoryException.invalidStayOperation(
          "Khi đổi phân công phòng vật lý phải cung cấp lý do thay đổi để lưu lịch sử kiểm toán (BK-16).");
    }

    Map<String, RoomAssignment> existingByItemAndRoom = new HashMap<>();
    for (RoomAssignment existing : currentActive) {
      existingByItemAndRoom.put(existing.bookingItemId() + ":" + existing.roomId(), existing);
    }

    Set<String> targetKeys = new HashSet<>();
    for (RoomAssignmentItem reqItem : command.assignments()) {
      for (UUID roomId : reqItem.roomIds()) {
        targetKeys.add(reqItem.bookingItemId() + ":" + roomId);
      }
    }

    List<RoomAssignment> toPersist = new ArrayList<>();
    String releaseReason =
        command.reason() != null && !command.reason().trim().isEmpty()
            ? command.reason().trim()
            : "Đổi phân công phòng vật lý";

    // Bảo toàn lịch sử phân công cũ bằng trạng thái RELEASED (BK-16)
    for (RoomAssignment existing : currentActive) {
      String key = existing.bookingItemId() + ":" + existing.roomId();
      if (!targetKeys.contains(key)) {
        toPersist.add(
            new RoomAssignment(
                existing.id(),
                existing.hotelId(),
                existing.bookingId(),
                existing.bookingItemId(),
                existing.roomTypeId(),
                existing.roomId(),
                existing.roomNumberSnapshot(),
                existing.startDate(),
                existing.endDate(),
                RoomAssignment.Status.RELEASED,
                existing.assignedByUserId(),
                existing.assignedAt(),
                existing.checkedInByUserId(),
                existing.checkedInAt(),
                existing.checkedOutByUserId(),
                existing.checkedOutAt(),
                command.actorId(),
                now,
                releaseReason));
      }
    }

    for (RoomAssignmentItem reqItem : command.assignments()) {
      BookingItemStayInfo bookingItem = itemsById.get(reqItem.bookingItemId());
      LocalDate effectiveStartDate = bookingItem.checkInDate();

      for (UUID roomId : reqItem.roomIds()) {
        String key = reqItem.bookingItemId() + ":" + roomId;
        if (!existingByItemAndRoom.containsKey(key)) {
          PhysicalRoomSummary room = roomsById.get(roomId);
          toPersist.add(
              new RoomAssignment(
                  UUID.randomUUID(),
                  command.hotelId(),
                  command.bookingId(),
                  reqItem.bookingItemId(),
                  bookingItem.roomTypeId(),
                  room.id(),
                  room.roomNumber(),
                  effectiveStartDate,
                  bookingItem.checkOutDate(),
                  RoomAssignment.Status.ASSIGNED,
                  command.actorId(),
                  now,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null));
        }
      }
    }

    if (!toPersist.isEmpty()) {
      roomAssignmentRepository.saveAll(toPersist);
      for (UUID roomTypeId : roomTypeIds) {
        if (!inventoryQueryPort.canAllocateAfterAssignment(
            roomTypeId, booking.checkInDate(), booking.checkOutDate())) {
          throw InventoryException.roomAssignmentConflict(
              "Không thể gán phòng vì các đơn giữ chỗ hoặc đã xác nhận khác không còn phòng phù hợp cho toàn bộ kỳ lưu trú.");
        }
      }
    }

    List<RoomAssignmentResult> activeResults =
        roomAssignmentRepository.findActiveByBookingId(command.bookingId()).stream()
            .map(RoomAssignmentResult::from)
            .toList();
    List<RoomAssignmentResult> historyResults =
        roomAssignmentRepository.findAllByBookingId(command.bookingId()).stream()
            .map(RoomAssignmentResult::from)
            .toList();

    return new BookingRoomAssignmentsResult(
        command.hotelId(), command.bookingId(), activeResults, historyResults);
  }
}

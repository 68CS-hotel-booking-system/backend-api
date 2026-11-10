package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand;
import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand.RoomAssignmentItem;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.PhysicalRoomSummary;
import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.usecase.AssignRoomsUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.PhysicalRoom;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.StayDemand;
import com.team.hotelbooking.inventory.infrastructure.persistence.adapter.JpaInventoryQueryAdapter;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
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
  private final InventoryReservationJpaRepository inventoryReservationJpaRepository;
  private final Clock clock;

  public AssignRoomsService(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      RoomBlockRepository roomBlockRepository,
      InventoryReservationJpaRepository inventoryReservationJpaRepository,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.bookingStayPort = bookingStayPort;
    this.roomAssignmentRepository = roomAssignmentRepository;
    this.roomBlockRepository = roomBlockRepository;
    this.inventoryReservationJpaRepository = inventoryReservationJpaRepository;
    this.clock = clock;
  }

  public AssignRoomsService(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      RoomBlockRepository roomBlockRepository,
      Clock clock) {
    this(
        hotelCatalogPort,
        bookingStayPort,
        roomAssignmentRepository,
        roomBlockRepository,
        null,
        clock);
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

    hotelCatalogPort
        .findHotelSummary(command.hotelId())
        .orElseThrow(() -> InventoryException.hotelNotFound("Không tìm thấy khách sạn."));
    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    BookingStayInfo booking =
        bookingStayPort
            .findBookingForStayOperation(command.hotelId(), command.bookingId())
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng đã xác nhận trong khách sạn."));

    if (booking.status() != StayStatus.CONFIRMED && booking.status() != StayStatus.CHECKED_IN) {
      throw InventoryException.invalidStayOperation(
          "Chỉ được phân hoặc đổi phòng vật lý cho đơn đặt phòng ở trạng thái CONFIRMED hoặc CHECKED_IN (BK-15).");
    }

    List<UUID> roomTypeIds =
        booking.items().stream().map(BookingItemStayInfo::roomTypeId).distinct().sorted().toList();
    hotelCatalogPort.lockRoomTypesInHotel(command.hotelId(), roomTypeIds);

    Instant now = clock.instant();
    LocalDate today = LocalDate.now(clock);

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

      LocalDate effectiveStartDate =
          booking.status() == StayStatus.CHECKED_IN && today.isAfter(bookingItem.checkInDate())
              ? today
              : bookingItem.checkInDate();
      if (!effectiveStartDate.isBefore(bookingItem.checkOutDate())) {
        effectiveStartDate = bookingItem.checkInDate();
      }

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

        if (booking.status() == StayStatus.CHECKED_IN
            && !"READY".equalsIgnoreCase(room.readiness())) {
          throw InventoryException.roomNotReady(
              "Phòng vật lý "
                  + room.roomNumber()
                  + " chưa ở trạng thái sẵn sàng (READY) để nhận hoặc đổi phòng cho khách đang lưu trú (BK-15).");
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

    // Bảo vệ các booking HELD/CONFIRMED chưa được gán phòng: đảm bảo sau khi gán,
    // các đơn khác vẫn còn đủ phương án phân bổ phòng liên tục hợp lệ (BK-15)
    for (UUID roomTypeId : roomTypeIds) {
      List<PhysicalRoomSummary> typeRooms =
          hotelCatalogPort.findPhysicalRoomsByRoomTypeId(roomTypeId);
      if (typeRooms.isEmpty()) {
        continue;
      }

      List<BookingItemStayInfo> itemsOfType =
          booking.items().stream().filter(i -> roomTypeId.equals(i.roomTypeId())).toList();
      LocalDate minDate =
          itemsOfType.stream()
              .map(BookingItemStayInfo::checkInDate)
              .min(LocalDate::compareTo)
              .orElse(today);
      LocalDate maxDate =
          itemsOfType.stream()
              .map(BookingItemStayInfo::checkOutDate)
              .max(LocalDate::compareTo)
              .orElse(today.plusDays(1));

      List<RoomBlock> activeBlocks =
          roomBlockRepository.findActiveBlocksInRange(roomTypeId, minDate, maxDate);

      List<RoomAssignment> otherAssignments =
          roomAssignmentRepository
              .findActiveByRoomTypeIdsOverlapping(List.of(roomTypeId), minDate, maxDate)
              .stream()
              .filter(a -> !command.bookingId().equals(a.bookingId()))
              .toList();

      Map<UUID, Set<LocalDate>> prospectiveOccupiedDatesByRoom = new HashMap<>();
      for (RoomAssignmentItem reqItem : command.assignments()) {
        BookingItemStayInfo bItem = itemsById.get(reqItem.bookingItemId());
        if (bItem != null && roomTypeId.equals(bItem.roomTypeId())) {
          LocalDate itemStart =
              booking.status() == StayStatus.CHECKED_IN && today.isAfter(bItem.checkInDate())
                  ? today
                  : bItem.checkInDate();
          if (!itemStart.isBefore(bItem.checkOutDate())) {
            itemStart = bItem.checkInDate();
          }
          for (UUID roomId : reqItem.roomIds()) {
            Set<LocalDate> dates =
                prospectiveOccupiedDatesByRoom.computeIfAbsent(roomId, k -> new HashSet<>());
            for (LocalDate d = itemStart; d.isBefore(bItem.checkOutDate()); d = d.plusDays(1)) {
              dates.add(d);
            }
          }
        }
      }

      List<PhysicalRoom> plannerRooms = new ArrayList<>();
      for (PhysicalRoomSummary r : typeRooms) {
        Set<LocalDate> occupiedDates = new HashSet<>();
        for (RoomBlock b : activeBlocks) {
          if (r.id().equals(b.roomId())) {
            for (LocalDate d = b.startDate(); !d.isAfter(b.endDate()); d = d.plusDays(1)) {
              occupiedDates.add(d);
            }
          }
        }
        for (RoomAssignment a : otherAssignments) {
          if (r.id().equals(a.roomId())) {
            for (LocalDate d = a.startDate(); d.isBefore(a.endDate()); d = d.plusDays(1)) {
              occupiedDates.add(d);
            }
          }
        }
        Set<LocalDate> currentAssigned = prospectiveOccupiedDatesByRoom.get(r.id());
        if (currentAssigned != null) {
          occupiedDates.addAll(currentAssigned);
        }

        plannerRooms.add(
            new PhysicalRoom(r.id(), r.operationalFrom(), r.operationalUntil(), occupiedDates));
      }

      if (inventoryReservationJpaRepository != null) {
        List<InventoryReservationJpaEntity> allReservations =
            inventoryReservationJpaRepository.findActiveReservationsInRange(
                roomTypeId, minDate, maxDate, now);
        List<InventoryReservationJpaEntity> otherReservations =
            allReservations.stream()
                .filter(res -> !command.bookingId().equals(res.getBookingId()))
                .toList();

        List<RoomAssignmentJpaEntity> otherAssignEntities =
            otherAssignments.stream()
                .map(
                    a -> {
                      RoomAssignmentJpaEntity ent = new RoomAssignmentJpaEntity();
                      ent.setBookingItemId(a.bookingItemId());
                      ent.setRoomId(a.roomId());
                      ent.setStartDate(a.startDate());
                      ent.setEndDate(a.endDate());
                      return ent;
                    })
                .toList();

        List<StayDemand> unassignedDemands =
            JpaInventoryQueryAdapter.buildStayDemandsFromReservations(
                otherReservations, otherAssignEntities);

        if (!unassignedDemands.isEmpty()) {
          boolean canAllocateOthers =
              RoomAllocationPlanner.canAllocate(plannerRooms, unassignedDemands, now);
          if (!canAllocateOthers) {
            throw InventoryException.roomAssignmentConflict(
                "Không thể gán phòng vật lý đã chọn vì làm mất khả năng phân bổ phòng liên tục cho các đơn đặt phòng khác đang chờ nhận phòng (BK-15).");
          }
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
        if (existing.status() == RoomAssignment.Status.CHECKED_IN) {
          hotelCatalogPort.updateRoomReadiness(
              existing.roomId(), existing.hotelId(), "DIRTY", command.actorId(), now);
        }
      }
    }

    RoomAssignment.Status newStatus =
        booking.status() == StayStatus.CHECKED_IN
            ? RoomAssignment.Status.CHECKED_IN
            : RoomAssignment.Status.ASSIGNED;

    for (RoomAssignmentItem reqItem : command.assignments()) {
      BookingItemStayInfo bookingItem = itemsById.get(reqItem.bookingItemId());
      LocalDate effectiveStartDate =
          booking.status() == StayStatus.CHECKED_IN && today.isAfter(bookingItem.checkInDate())
              ? today
              : bookingItem.checkInDate();
      if (!effectiveStartDate.isBefore(bookingItem.checkOutDate())) {
        effectiveStartDate = bookingItem.checkInDate();
      }

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
                  newStatus,
                  command.actorId(),
                  now,
                  newStatus == RoomAssignment.Status.CHECKED_IN ? command.actorId() : null,
                  newStatus == RoomAssignment.Status.CHECKED_IN ? now : null,
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

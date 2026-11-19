package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.GetRoomCalendarQuery;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult.DailyInventorySummaryResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult.PhysicalRoomCalendarResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult.RoomTypeCalendarResult;
import com.team.hotelbooking.inventory.application.usecase.GetRoomCalendarUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.PhysicalRoom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GetRoomCalendarService implements GetRoomCalendarUseCase {

  private static final int MAX_CALENDAR_DAYS = 90;

  private final HotelCatalogPort hotelCatalogPort;
  private final RoomBlockRepository roomBlockRepository;
  private final RoomAssignmentRepository roomAssignmentRepository;
  private final InventoryQueryPort inventoryQueryPort;
  private final Clock clock;

  public GetRoomCalendarService(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock) {
    this(hotelCatalogPort, roomBlockRepository, null, inventoryQueryPort, clock);
  }

  public GetRoomCalendarService(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      RoomAssignmentRepository roomAssignmentRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.roomBlockRepository = roomBlockRepository;
    this.roomAssignmentRepository = roomAssignmentRepository;
    this.inventoryQueryPort = inventoryQueryPort;
    this.clock = clock;
  }

  @Override
  public RoomCalendarResult execute(GetRoomCalendarQuery query) {
    if (query == null
        || query.hotelId() == null
        || query.startDate() == null
        || query.endDate() == null) {
      throw InventoryException.invalidDateRange(
          "Mã khách sạn và khoảng ngày tra cứu lịch phòng không được để trống.");
    }

    if (query.endDate().isBefore(query.startDate())) {
      throw InventoryException.invalidDateRange("Ngày kết thúc không được trước ngày bắt đầu.");
    }

    long days = ChronoUnit.DAYS.between(query.startDate(), query.endDate());
    if (days > MAX_CALENDAR_DAYS) {
      throw InventoryException.maxStayExceeded(
          "Khoảng thời gian tra cứu lịch phòng tối đa là 90 ngày.");
    }

    hotelCatalogPort
        .findHotelSummary(query.hotelId())
        .orElseThrow(() -> InventoryException.hotelNotFound("Không tìm thấy khách sạn."));
    if (query.actorId() != null) {
      hotelCatalogPort.requireManagementAccess(query.actorId(), query.hotelId());
    }

    List<HotelCatalogPort.RoomTypeSummary> roomTypes =
        hotelCatalogPort.findActiveRoomTypes(query.hotelId());
    if (query.roomTypeId() != null) {
      roomTypes = roomTypes.stream().filter(rt -> query.roomTypeId().equals(rt.id())).toList();
      if (roomTypes.isEmpty()) {
        throw InventoryException.roomTypeInactive(
            "Không tìm thấy loại phòng thuộc khách sạn hoặc loại phòng đã ngừng bán.");
      }
    }

    List<HotelCatalogPort.PhysicalRoomSummary> allRooms =
        hotelCatalogPort.findPhysicalRoomsByHotelId(query.hotelId());

    List<RoomBlock> activeBlocks =
        roomBlockRepository
            .findManagementBlocks(
                query.hotelId(), query.roomTypeId(), null, query.startDate(), query.endDate())
            .stream()
            .filter(b -> b.status() == RoomBlock.Status.ACTIVE)
            .toList();

    Map<UUID, List<RoomBlock>> blocksByRoomId = new HashMap<>();
    for (RoomBlock block : activeBlocks) {
      if (block.roomId() != null) {
        blocksByRoomId.computeIfAbsent(block.roomId(), k -> new ArrayList<>()).add(block);
      }
    }

    List<RoomAssignment> activeAssignments =
        roomAssignmentRepository != null
            ? roomAssignmentRepository.findActiveByHotelIdOverlapping(
                query.hotelId(), query.startDate(), query.endDate().plusDays(1))
            : List.of();
    Map<UUID, List<RoomAssignment>> assignmentsByRoomId = new HashMap<>();
    if (activeAssignments != null) {
      for (RoomAssignment assignment : activeAssignments) {
        if (assignment.roomId() != null) {
          assignmentsByRoomId
              .computeIfAbsent(assignment.roomId(), k -> new ArrayList<>())
              .add(assignment);
        }
      }
    }

    Instant now = clock.instant();
    List<RoomTypeCalendarResult> roomTypeResults = new ArrayList<>();

    for (HotelCatalogPort.RoomTypeSummary rt : roomTypes) {
      List<HotelCatalogPort.PhysicalRoomSummary> rtRooms =
          allRooms.stream().filter(r -> rt.id().equals(r.roomTypeId())).toList();

      List<PhysicalRoomCalendarResult> roomCalendarList = new ArrayList<>();
      Map<UUID, PhysicalRoom> plannerRoomsById = new HashMap<>();

      for (HotelCatalogPort.PhysicalRoomSummary r : rtRooms) {
        List<RoomBlock> rBlocks = blocksByRoomId.getOrDefault(r.id(), List.of());
        List<RoomAssignment> rAssignments = assignmentsByRoomId.getOrDefault(r.id(), List.of());
        Set<LocalDate> blockedDates = new HashSet<>();
        for (RoomBlock b : rBlocks) {
          LocalDate start =
              b.startDate().isBefore(query.startDate()) ? query.startDate() : b.startDate();
          LocalDate end = b.endDate().isAfter(query.endDate()) ? query.endDate() : b.endDate();
          for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            blockedDates.add(d);
          }
        }
        plannerRoomsById.put(
            r.id(),
            new PhysicalRoom(r.id(), r.operationalFrom(), r.operationalUntil(), blockedDates));

        roomCalendarList.add(
            new PhysicalRoomCalendarResult(
                r.id(),
                r.roomNumber(),
                r.floor(),
                r.readiness(),
                r.operationalFrom(),
                r.operationalUntil(),
                rBlocks.stream().map(RoomBlockResult::from).toList(),
                rAssignments.stream().map(RoomAssignmentResult::from).toList()));
      }

      List<DailyInventorySummaryResult> dailySummaries = new ArrayList<>();
      for (LocalDate date = query.startDate();
          !date.isAfter(query.endDate());
          date = date.plusDays(1)) {
        int operationalCount = 0;
        int blockedCount = 0;
        for (HotelCatalogPort.PhysicalRoomSummary r : rtRooms) {
          PhysicalRoom pr = plannerRoomsById.get(r.id());
          PhysicalRoom unblockedProbe =
              new PhysicalRoom(r.id(), r.operationalFrom(), r.operationalUntil(), Set.of());
          if (unblockedProbe.isAvailableOn(date, now)) {
            operationalCount++;
            if (!pr.isAvailableOn(date, now)) {
              blockedCount++;
            }
          }
        }

        int availableOnDate =
            inventoryQueryPort.calculateAvailableQuantity(rt.id(), date, date.plusDays(1));
        int reservedOnDate = Math.max(0, operationalCount - blockedCount - availableOnDate);

        dailySummaries.add(
            new DailyInventorySummaryResult(
                date, operationalCount, blockedCount, reservedOnDate, availableOnDate));
      }

      roomTypeResults.add(
          new RoomTypeCalendarResult(rt.id(), rt.name(), roomCalendarList, dailySummaries));
    }

    return new RoomCalendarResult(
        query.hotelId(), query.startDate(), query.endDate(), roomTypeResults);
  }
}

package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.PhysicalRoom;
import com.team.hotelbooking.inventory.domain.service.RoomAllocationPlanner.StayDemand;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.DailyReservationOccupancy;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomAssignmentJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JpaInventoryQueryAdapter implements InventoryQueryPort {

  private static final int MAX_STAY_HORIZON_DAYS = 30;

  private final HotelCatalogPort hotelCatalogPort;
  private final RoomBlockJpaRepository roomBlockJpaRepository;
  private final InventoryReservationJpaRepository reservationJpaRepository;
  private final RoomAssignmentJpaRepository roomAssignmentJpaRepository;
  private final Clock clock;

  public JpaInventoryQueryAdapter(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockJpaRepository roomBlockJpaRepository,
      InventoryReservationJpaRepository reservationJpaRepository,
      Clock clock) {
    this(hotelCatalogPort, roomBlockJpaRepository, reservationJpaRepository, null, clock);
  }

  @Autowired
  public JpaInventoryQueryAdapter(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockJpaRepository roomBlockJpaRepository,
      InventoryReservationJpaRepository reservationJpaRepository,
      RoomAssignmentJpaRepository roomAssignmentJpaRepository,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.roomBlockJpaRepository = roomBlockJpaRepository;
    this.reservationJpaRepository = reservationJpaRepository;
    this.roomAssignmentJpaRepository = roomAssignmentJpaRepository;
    this.clock = clock;
  }

  @Override
  public int calculateAvailableQuantity(
      UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {

    List<HotelCatalogPort.PhysicalRoomSummary> physicalRooms =
        hotelCatalogPort.findPhysicalRoomsByRoomTypeId(roomTypeId);

    if (physicalRooms == null || physicalRooms.isEmpty()) {
      long totalRooms = hotelCatalogPort.countActiveRoomsByRoomTypeId(roomTypeId);
      if (totalRooms <= 0) {
        return 0;
      }
      List<HotelCatalogPort.PhysicalRoomSummary> synthesized = new ArrayList<>((int) totalRooms);
      for (int i = 0; i < totalRooms; i++) {
        synthesized.add(
            new HotelCatalogPort.PhysicalRoomSummary(
                UUID.randomUUID(), null, roomTypeId, String.valueOf(i + 1), null, null, null));
      }
      physicalRooms = synthesized;
    }

    Instant now = clock.instant();
    LocalDate horizonStart = checkInDate.minusDays(MAX_STAY_HORIZON_DAYS);
    LocalDate horizonEnd = checkOutDate.plusDays(MAX_STAY_HORIZON_DAYS);

    List<RoomBlockJpaEntity> blocks =
        roomBlockJpaRepository.findActiveBlocksInRange(roomTypeId, horizonStart, horizonEnd);
    if (blocks == null || blocks.isEmpty())
      blocks =
          roomBlockJpaRepository.findActiveBlocksInRange(roomTypeId, checkInDate, checkOutDate);
    List<RoomAssignmentJpaEntity> assignments =
        roomAssignmentJpaRepository == null
            ? List.of()
            : roomAssignmentJpaRepository.findActiveByRoomTypeIdsOverlapping(
                List.of(roomTypeId), horizonStart, horizonEnd);
    List<InventoryReservationJpaEntity> reservations =
        reservationJpaRepository.findActiveReservationsInRange(
            roomTypeId, horizonStart, horizonEnd, now);
    List<DailyReservationOccupancy> occupancy =
        reservations == null || reservations.isEmpty()
            ? reservationJpaRepository.sumOccupancyByDateRange(
                roomTypeId, checkInDate, checkOutDate, now)
            : List.of();
    return calculateContinuous(
        physicalRooms,
        blocks == null ? List.of() : blocks,
        assignments == null ? List.of() : assignments,
        reservations,
        occupancy,
        checkInDate,
        checkOutDate,
        now);
  }

  private int calculateContinuous(
      List<HotelCatalogPort.PhysicalRoomSummary> physicalRooms,
      List<RoomBlockJpaEntity> activeBlocks,
      List<RoomAssignmentJpaEntity> activeAssignments,
      List<InventoryReservationJpaEntity> activeReservations,
      List<DailyReservationOccupancy> occupancyList,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Instant now) {
    LocalDate horizonStart = checkInDate.minusDays(MAX_STAY_HORIZON_DAYS);
    LocalDate horizonEnd = checkOutDate.plusDays(MAX_STAY_HORIZON_DAYS);
    // 1. Lấy tất cả room blocks còn ACTIVE giao thoa với khoảng xét
    Map<UUID, Set<LocalDate>> blockedDatesByRoom = new HashMap<>();
    List<StayDemand> allDemands = new ArrayList<>();

    for (RoomBlockJpaEntity block : activeBlocks) {
      LocalDate start =
          block.getStartDate().isBefore(horizonStart) ? horizonStart : block.getStartDate();
      LocalDate endInclusive =
          block.getEndDate().isAfter(horizonEnd.minusDays(1))
              ? horizonEnd.minusDays(1)
              : block.getEndDate();
      if (endInclusive.isBefore(start)) {
        continue;
      }
      if (block.getRoomId() != null) {
        Set<LocalDate> dates =
            blockedDatesByRoom.computeIfAbsent(block.getRoomId(), k -> new HashSet<>());
        for (LocalDate d = start; !d.isAfter(endInclusive); d = d.plusDays(1)) {
          dates.add(d);
        }
      } else {
        allDemands.add(new StayDemand(start, endInclusive.plusDays(1), 1));
      }
    }

    for (RoomAssignmentJpaEntity assignment : activeAssignments) {
      LocalDate start =
          assignment.getStartDate().isBefore(horizonStart)
              ? horizonStart
              : assignment.getStartDate();
      LocalDate endExclusive =
          assignment.getEndDate().isAfter(horizonEnd) ? horizonEnd : assignment.getEndDate();
      Set<LocalDate> dates =
          blockedDatesByRoom.computeIfAbsent(assignment.getRoomId(), k -> new HashSet<>());
      for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
        dates.add(d);
      }
    }

    List<PhysicalRoom> plannerRooms = new ArrayList<>(physicalRooms.size());
    for (HotelCatalogPort.PhysicalRoomSummary room : physicalRooms) {
      Set<LocalDate> blockedDates = blockedDatesByRoom.getOrDefault(room.id(), Set.of());
      plannerRooms.add(
          new PhysicalRoom(
              room.id(), room.operationalFrom(), room.operationalUntil(), blockedDates));
    }

    // 2. Lấy các giữ chỗ (HELD còn hạn) và đã xác nhận (CONFIRMED) để ghép thành khoảng lưu trú
    if (activeReservations != null && !activeReservations.isEmpty()) {
      allDemands.addAll(buildStayDemandsFromReservations(activeReservations, activeAssignments));
    } else {
      if (occupancyList != null) {
        for (DailyReservationOccupancy occ : occupancyList) {
          if (occ.getTotalQuantity() > 0) {
            allDemands.add(
                new StayDemand(
                    occ.getStayDate(),
                    occ.getStayDate().plusDays(1),
                    (int) occ.getTotalQuantity()));
          }
        }
      }
    }

    List<StayDemand> relevantDemands =
        filterConnectedDemands(allDemands, checkInDate, checkOutDate);

    // 3. Kiểm tra phân bổ liên tục cả kỳ theo IV-03, IV-04, IV-13
    return RoomAllocationPlanner.calculateMaxAllocatable(
        plannerRooms, relevantDemands, checkInDate, checkOutDate, now);
  }

  @Override
  public boolean canAllocateAfterAssignment(
      UUID roomTypeId, LocalDate startDate, LocalDate endDateExclusive) {
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, clock.getZone());
    LocalDate firstAffectedNight = startDate.isAfter(today) ? startDate : today;
    if (!endDateExclusive.isAfter(firstAffectedNight)) {
      return true;
    }

    List<InventoryReservationJpaEntity> reservations =
        reservationJpaRepository.findActiveReservationsFrom(
            roomTypeId, today, now, DbReservationStatus.HELD, DbReservationStatus.CONFIRMED);
    List<StayDemand> connectedStays =
        filterConnectedDemands(
            buildStayDemandsFromReservations(reservations), firstAffectedNight, endDateExclusive);
    if (connectedStays.isEmpty()) {
      return true;
    }

    // Lấy cả các đêm nằm ngoài khoảng gán nếu chúng thuộc kỳ ở bị ảnh hưởng.
    LocalDate start =
        connectedStays.stream()
            .map(StayDemand::startInclusive)
            .min(LocalDate::compareTo)
            .orElseThrow();
    LocalDate end =
        connectedStays.stream()
            .map(StayDemand::endExclusive)
            .max(LocalDate::compareTo)
            .orElseThrow();
    List<RoomAssignmentJpaEntity> assignments =
        roomAssignmentJpaRepository.findActiveByRoomTypeIdsOverlapping(
            List.of(roomTypeId), start, end);
    List<StayDemand> demands =
        new ArrayList<>(
            filterConnectedDemands(
                buildStayDemandsFromReservations(reservations, assignments), start, end));
    Map<UUID, Set<LocalDate>> occupiedDates = new HashMap<>();
    for (RoomAssignmentJpaEntity assignment : assignments) {
      addOccupiedNights(
          occupiedDates,
          assignment.getRoomId(),
          assignment.getStartDate(),
          assignment.getEndDate(),
          start,
          end);
    }
    for (RoomBlockJpaEntity block :
        roomBlockJpaRepository.findActiveBlocksInRange(roomTypeId, start, end)) {
      LocalDate blockEndExclusive = block.getEndDate().plusDays(1);
      if (block.getRoomId() == null) {
        LocalDate overlapStart = block.getStartDate().isAfter(start) ? block.getStartDate() : start;
        LocalDate overlapEnd = blockEndExclusive.isBefore(end) ? blockEndExclusive : end;
        demands.add(new StayDemand(overlapStart, overlapEnd, 1));
      } else {
        addOccupiedNights(
            occupiedDates, block.getRoomId(), block.getStartDate(), blockEndExclusive, start, end);
      }
    }
    List<PhysicalRoom> rooms =
        hotelCatalogPort.findPhysicalRoomsByRoomTypeId(roomTypeId).stream()
            .map(
                room ->
                    new PhysicalRoom(
                        room.id(),
                        room.operationalFrom(),
                        room.operationalUntil(),
                        occupiedDates.getOrDefault(room.id(), Set.of())))
            .toList();
    return RoomAllocationPlanner.canAllocate(rooms, demands, now);
  }

  private static void addOccupiedNights(
      Map<UUID, Set<LocalDate>> datesByRoom,
      UUID roomId,
      LocalDate occupiedStart,
      LocalDate occupiedEnd,
      LocalDate rangeStart,
      LocalDate rangeEnd) {
    LocalDate start = occupiedStart.isAfter(rangeStart) ? occupiedStart : rangeStart;
    LocalDate end = occupiedEnd.isBefore(rangeEnd) ? occupiedEnd : rangeEnd;
    Set<LocalDate> dates = datesByRoom.computeIfAbsent(roomId, ignored -> new HashSet<>());
    for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
      dates.add(date);
    }
  }

  @Override
  public boolean canAllocateWithCandidateBlock(
      UUID roomTypeId,
      UUID candidateBlockIdToReplace,
      UUID roomId,
      LocalDate blockStartDate,
      LocalDate blockEndDate) {

    List<HotelCatalogPort.PhysicalRoomSummary> physicalRooms =
        hotelCatalogPort.findPhysicalRoomsByRoomTypeId(roomTypeId);
    if (physicalRooms == null || physicalRooms.isEmpty()) {
      return false;
    }

    Instant now = clock.instant();
    LocalDate exclusiveEnd = blockEndDate.plusDays(1);
    LocalDate horizonStart = blockStartDate.minusDays(MAX_STAY_HORIZON_DAYS);
    LocalDate horizonEnd = exclusiveEnd.plusDays(MAX_STAY_HORIZON_DAYS);

    if (roomAssignmentJpaRepository != null) {
      List<RoomAssignmentJpaEntity> overlappingOnTargetRoom =
          roomAssignmentJpaRepository.findActiveByRoomIdOverlapping(
              roomId, blockStartDate, exclusiveEnd);
      if (overlappingOnTargetRoom != null && !overlappingOnTargetRoom.isEmpty()) {
        return false;
      }
    }

    List<RoomBlockJpaEntity> activeBlocks =
        roomBlockJpaRepository.findActiveBlocksInRange(roomTypeId, horizonStart, horizonEnd);
    if (activeBlocks == null) {
      activeBlocks = List.of();
    }

    Map<UUID, Set<LocalDate>> blockedDatesByRoom = new HashMap<>();
    List<StayDemand> allDemands = new ArrayList<>();

    for (RoomBlockJpaEntity block : activeBlocks) {
      if (candidateBlockIdToReplace != null && candidateBlockIdToReplace.equals(block.getId())) {
        continue;
      }
      LocalDate start =
          block.getStartDate().isBefore(horizonStart) ? horizonStart : block.getStartDate();
      LocalDate endInclusive =
          block.getEndDate().isAfter(horizonEnd.minusDays(1))
              ? horizonEnd.minusDays(1)
              : block.getEndDate();
      if (endInclusive.isBefore(start)) {
        continue;
      }
      if (block.getRoomId() != null) {
        Set<LocalDate> dates =
            blockedDatesByRoom.computeIfAbsent(block.getRoomId(), k -> new HashSet<>());
        for (LocalDate d = start; !d.isAfter(endInclusive); d = d.plusDays(1)) {
          dates.add(d);
        }
      } else {
        allDemands.add(new StayDemand(start, endInclusive.plusDays(1), 1));
      }
    }

    List<RoomAssignmentJpaEntity> activeAssignments =
        roomAssignmentJpaRepository != null
            ? roomAssignmentJpaRepository.findActiveByRoomTypeIdsOverlapping(
                List.of(roomTypeId), horizonStart, horizonEnd)
            : List.of();
    if (activeAssignments == null) {
      activeAssignments = List.of();
    }
    for (RoomAssignmentJpaEntity assignment : activeAssignments) {
      LocalDate start =
          assignment.getStartDate().isBefore(horizonStart)
              ? horizonStart
              : assignment.getStartDate();
      LocalDate endExclusive =
          assignment.getEndDate().isAfter(horizonEnd) ? horizonEnd : assignment.getEndDate();
      Set<LocalDate> dates =
          blockedDatesByRoom.computeIfAbsent(assignment.getRoomId(), k -> new HashSet<>());
      for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
        dates.add(d);
      }
    }

    Set<LocalDate> candidateDates =
        blockedDatesByRoom.computeIfAbsent(roomId, k -> new HashSet<>());
    for (LocalDate d = blockStartDate; !d.isAfter(blockEndDate); d = d.plusDays(1)) {
      candidateDates.add(d);
    }

    List<PhysicalRoom> plannerRooms = new ArrayList<>(physicalRooms.size());
    for (HotelCatalogPort.PhysicalRoomSummary room : physicalRooms) {
      Set<LocalDate> blockedDates = blockedDatesByRoom.getOrDefault(room.id(), Set.of());
      plannerRooms.add(
          new PhysicalRoom(
              room.id(), room.operationalFrom(), room.operationalUntil(), blockedDates));
    }

    List<InventoryReservationJpaEntity> activeReservations =
        reservationJpaRepository.findActiveReservationsInRange(
            roomTypeId, horizonStart, horizonEnd, now);
    if (activeReservations != null && !activeReservations.isEmpty()) {
      allDemands.addAll(buildStayDemandsFromReservations(activeReservations, activeAssignments));
    }

    List<StayDemand> relevantDemands =
        filterConnectedDemands(allDemands, blockStartDate, exclusiveEnd);

    return RoomAllocationPlanner.canAllocate(plannerRooms, relevantDemands, now);
  }

  public static List<StayDemand> buildStayDemandsFromReservations(
      List<InventoryReservationJpaEntity> reservations) {
    return buildStayDemandsFromReservations(reservations, List.of());
  }

  public static List<StayDemand> buildStayDemandsFromReservations(
      List<InventoryReservationJpaEntity> reservations,
      List<RoomAssignmentJpaEntity> activeAssignments) {
    List<StayDemand> demands = new ArrayList<>();
    Map<UUID, TreeMap<LocalDate, Integer>> byBookingItem = new LinkedHashMap<>();

    for (InventoryReservationJpaEntity res : reservations) {
      int qty = res.getQuantity() != null ? res.getQuantity() : 0;
      if (qty <= 0 || res.getStayDate() == null) {
        continue;
      }
      if (res.getBookingItemId() == null) {
        demands.add(new StayDemand(res.getStayDate(), res.getStayDate().plusDays(1), qty));
      } else {
        byBookingItem
            .computeIfAbsent(res.getBookingItemId(), k -> new TreeMap<>())
            .merge(res.getStayDate(), qty, Integer::sum);
      }
    }

    // Trừ đi số lượng phòng đã được phân công cụ thể cho từng bookingItemId để không trừ tồn 2 lần
    // (BK-17)
    if (activeAssignments != null) {
      for (RoomAssignmentJpaEntity assignment : activeAssignments) {
        TreeMap<LocalDate, Integer> itemDates = byBookingItem.get(assignment.getBookingItemId());
        if (itemDates != null) {
          for (LocalDate d = assignment.getStartDate();
              d.isBefore(assignment.getEndDate());
              d = d.plusDays(1)) {
            Integer currentQty = itemDates.get(d);
            if (currentQty != null) {
              if (currentQty <= 1) {
                itemDates.remove(d);
              } else {
                itemDates.put(d, currentQty - 1);
              }
            }
          }
        }
      }
    }

    for (TreeMap<LocalDate, Integer> dateMap : byBookingItem.values()) {
      LocalDate runStart = null;
      LocalDate prevDate = null;
      int runQty = 0;

      for (Map.Entry<LocalDate, Integer> entry : dateMap.entrySet()) {
        LocalDate d = entry.getKey();
        int q = entry.getValue();
        if (runStart == null) {
          runStart = d;
          prevDate = d;
          runQty = q;
        } else if (d.equals(prevDate.plusDays(1)) && q == runQty) {
          prevDate = d;
        } else {
          demands.add(new StayDemand(runStart, prevDate.plusDays(1), runQty));
          runStart = d;
          prevDate = d;
          runQty = q;
        }
      }
      if (runStart != null) {
        demands.add(new StayDemand(runStart, prevDate.plusDays(1), runQty));
      }
    }

    return demands;
  }

  private static List<StayDemand> filterConnectedDemands(
      List<StayDemand> demands, LocalDate checkInDate, LocalDate checkOutDate) {
    if (demands.isEmpty()) {
      return List.of();
    }
    LocalDate compStart = checkInDate;
    LocalDate compEnd = checkOutDate;
    boolean expanded = true;
    while (expanded) {
      expanded = false;
      for (StayDemand d : demands) {
        if (d.startInclusive().isBefore(compEnd) && d.endExclusive().isAfter(compStart)) {
          if (d.startInclusive().isBefore(compStart)) {
            compStart = d.startInclusive();
            expanded = true;
          }
          if (d.endExclusive().isAfter(compEnd)) {
            compEnd = d.endExclusive();
            expanded = true;
          }
        }
      }
    }

    final LocalDate finalStart = compStart;
    final LocalDate finalEnd = compEnd;
    return demands.stream()
        .filter(d -> d.startInclusive().isBefore(finalEnd) && d.endExclusive().isAfter(finalStart))
        .toList();
  }

  @Override
  public Map<UUID, Integer> calculateMinAvailableQuantities(
      Map<UUID, Long> operationalRoomsByRoomType, LocalDate checkInDate, LocalDate checkOutDate) {
    if (operationalRoomsByRoomType.isEmpty()) {
      return Map.of();
    }

    Instant now = clock.instant();
    var ids = operationalRoomsByRoomType.keySet();
    LocalDate start = checkInDate.minusDays(MAX_STAY_HORIZON_DAYS);
    LocalDate end = checkOutDate.plusDays(MAX_STAY_HORIZON_DAYS);
    var rooms =
        hotelCatalogPort.findPhysicalRoomsByRoomTypeIds(ids).stream()
            .collect(Collectors.groupingBy(HotelCatalogPort.PhysicalRoomSummary::roomTypeId));
    var blocks =
        roomBlockJpaRepository.findActiveBlocksInRangeForRoomTypes(ids, start, end).stream()
            .collect(Collectors.groupingBy(RoomBlockJpaEntity::getRoomTypeId));
    var reservations =
        reservationJpaRepository
            .findActiveReservationsForRoomTypes(
                ids, start, end, now, DbReservationStatus.HELD, DbReservationStatus.CONFIRMED)
            .stream()
            .collect(Collectors.groupingBy(InventoryReservationJpaEntity::getRoomTypeId));
    var assignments =
        roomAssignmentJpaRepository == null
            ? Map.<UUID, List<RoomAssignmentJpaEntity>>of()
            : roomAssignmentJpaRepository
                .findActiveByRoomTypeIdsOverlapping(ids, start, end)
                .stream()
                .collect(Collectors.groupingBy(RoomAssignmentJpaEntity::getRoomTypeId));
    Map<UUID, Integer> result = new HashMap<>();
    for (UUID id : ids) {
      result.put(
          id,
          calculateContinuous(
              rooms.getOrDefault(id, List.of()),
              blocks.getOrDefault(id, List.of()),
              assignments.getOrDefault(id, List.of()),
              reservations.getOrDefault(id, List.of()),
              List.of(),
              checkInDate,
              checkOutDate,
              now));
    }
    return result;
  }
}

package com.team.hotelbooking.inventory.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bộ kiểm tra và tính toán khả năng phân bổ phòng vật lý liên tục cho cả kỳ lưu trú (IV-01, IV-03,
 * IV-04, IV-12, IV-13, HT-12).
 */
public final class RoomAllocationPlanner {

  private static final int MAX_BACKTRACK_STEPS = 10_000;

  private RoomAllocationPlanner() {}

  public record PhysicalRoom(
      UUID id, Instant operationalFrom, Instant operationalUntil, Set<LocalDate> blockedDates) {

    public boolean isAvailableOn(LocalDate date, Instant now) {
      if (operationalFrom != null
          && operationalFrom.atZone(ZoneOffset.UTC).toLocalDate().isAfter(date)) {
        return false;
      }
      if (operationalUntil != null) {
        Instant nightStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        if (!operationalUntil.isAfter(nightStart)) {
          return false;
        }
        LocalDate dateOfNow = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (!date.isBefore(dateOfNow) && !operationalUntil.isAfter(now)) {
          return false;
        }
      }
      return blockedDates == null || !blockedDates.contains(date);
    }

    public boolean isAvailableForRange(
        LocalDate startInclusive, LocalDate endExclusive, Instant now) {
      for (LocalDate date = startInclusive; date.isBefore(endExclusive); date = date.plusDays(1)) {
        if (!isAvailableOn(date, now)) {
          return false;
        }
      }
      return true;
    }
  }

  public record StayDemand(LocalDate startInclusive, LocalDate endExclusive, int quantity) {}

  private record UnitInterval(int startDay, int endDay) {}

  /**
   * Tính số lượng phòng tối đa có thể nhận thêm liên tục trong khoảng [checkInDate, checkOutDate)
   * mà vẫn bảo đảm phương án phân bổ phòng vật lý liên tục cho mọi nhu cầu đang giữ/đã xác nhận.
   */
  public static int calculateMaxAllocatable(
      List<PhysicalRoom> rooms,
      List<StayDemand> existingDemands,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Instant now) {

    if (rooms == null
        || rooms.isEmpty()
        || checkInDate == null
        || checkOutDate == null
        || !checkOutDate.isAfter(checkInDate)) {
      return 0;
    }

    List<StayDemand> activeDemands =
        existingDemands == null
            ? List.of()
            : existingDemands.stream()
                .filter(
                    d ->
                        d != null
                            && d.quantity() > 0
                            && d.startInclusive() != null
                            && d.endExclusive() != null
                            && d.endExclusive().isAfter(d.startInclusive()))
                .toList();

    int continuouslyFreeRooms = 0;
    for (PhysicalRoom room : rooms) {
      if (room.isAvailableForRange(checkInDate, checkOutDate, now)) {
        continuouslyFreeRooms++;
      }
    }
    if (continuouslyFreeRooms == 0) {
      return 0;
    }

    if (activeDemands.isEmpty()) {
      return continuouslyFreeRooms;
    }

    int upperBound = continuouslyFreeRooms;
    for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
      int freeOnDate = 0;
      for (PhysicalRoom room : rooms) {
        if (room.isAvailableOn(date, now)) {
          freeOnDate++;
        }
      }
      int demandedOnDate = 0;
      for (StayDemand demand : activeDemands) {
        if (!date.isBefore(demand.startInclusive()) && date.isBefore(demand.endExclusive())) {
          demandedOnDate += demand.quantity();
        }
      }
      int remainingOnDate = freeOnDate - demandedOnDate;
      if (remainingOnDate <= 0) {
        return 0;
      }
      if (remainingOnDate < upperBound) {
        upperBound = remainingOnDate;
      }
    }

    if (canAllocateWithAdditional(
        rooms, activeDemands, checkInDate, checkOutDate, upperBound, now)) {
      return upperBound;
    }

    int low = 1;
    int high = upperBound - 1;
    int best = 0;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      if (canAllocateWithAdditional(rooms, activeDemands, checkInDate, checkOutDate, mid, now)) {
        best = mid;
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return best;
  }

  /**
   * Kiểm tra xem toàn bộ danh sách nhu cầu lưu trú có thể phân bổ vào các phòng vật lý liên tục cả
   * kỳ hay không.
   */
  public static boolean canAllocate(
      List<PhysicalRoom> rooms, List<StayDemand> demands, Instant now) {
    if (demands == null || demands.isEmpty()) {
      return true;
    }
    if (rooms == null || rooms.isEmpty()) {
      return false;
    }

    List<StayDemand> validDemands =
        demands.stream()
            .filter(
                d ->
                    d != null
                        && d.quantity() > 0
                        && d.startInclusive() != null
                        && d.endExclusive() != null
                        && d.endExclusive().isAfter(d.startInclusive()))
            .toList();

    if (validDemands.isEmpty()) {
      return true;
    }

    LocalDate minDate = validDemands.get(0).startInclusive();
    LocalDate maxDate = validDemands.get(0).endExclusive();
    for (StayDemand d : validDemands) {
      if (d.startInclusive().isBefore(minDate)) {
        minDate = d.startInclusive();
      }
      if (d.endExclusive().isAfter(maxDate)) {
        maxDate = d.endExclusive();
      }
    }

    int horizonDays = (int) ChronoUnit.DAYS.between(minDate, maxDate);
    if (horizonDays <= 0) {
      return true;
    }

    int roomCount = rooms.size();
    BitSet[] roomMasks = new BitSet[roomCount];
    int[] freeRoomsPerDay = new int[horizonDays];

    for (int r = 0; r < roomCount; r++) {
      BitSet mask = new BitSet(horizonDays);
      PhysicalRoom room = rooms.get(r);
      for (int day = 0; day < horizonDays; day++) {
        LocalDate date = minDate.plusDays(day);
        if (room.isAvailableOn(date, now)) {
          freeRoomsPerDay[day]++;
        } else {
          mask.set(day);
        }
      }
      roomMasks[r] = mask;
    }

    int[] demandedPerDay = new int[horizonDays];
    List<UnitInterval> unitIntervals = new ArrayList<>();

    for (StayDemand demand : validDemands) {
      int startDay = (int) ChronoUnit.DAYS.between(minDate, demand.startInclusive());
      int endDay = (int) ChronoUnit.DAYS.between(minDate, demand.endExclusive());
      for (int day = startDay; day < endDay; day++) {
        demandedPerDay[day] += demand.quantity();
        if (demandedPerDay[day] > freeRoomsPerDay[day]) {
          return false;
        }
      }
      for (int q = 0; q < demand.quantity(); q++) {
        unitIntervals.add(new UnitInterval(startDay, endDay));
      }
    }

    unitIntervals.sort(
        Comparator.comparingInt(UnitInterval::startDay)
            .thenComparing(Comparator.comparingInt(UnitInterval::endDay).reversed()));

    int[] stepCounter = new int[] {0};
    return backtrack(unitIntervals, 0, roomMasks, horizonDays, stepCounter);
  }

  private static boolean canAllocateWithAdditional(
      List<PhysicalRoom> rooms,
      List<StayDemand> activeDemands,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      int additionalQty,
      Instant now) {
    List<StayDemand> combined = new ArrayList<>(activeDemands.size() + 1);
    combined.addAll(activeDemands);
    combined.add(new StayDemand(checkInDate, checkOutDate, additionalQty));
    return canAllocate(rooms, combined, now);
  }

  private static boolean backtrack(
      List<UnitInterval> intervals,
      int index,
      BitSet[] roomMasks,
      int horizonDays,
      int[] stepCounter) {
    if (index >= intervals.size()) {
      return true;
    }
    if (++stepCounter[0] > MAX_BACKTRACK_STEPS) {
      return false;
    }

    UnitInterval current = intervals.get(index);
    int start = current.startDay();
    int end = current.endDay();

    List<Integer> candidates = new ArrayList<>();
    for (int r = 0; r < roomMasks.length; r++) {
      int nextBusy = roomMasks[r].nextSetBit(start);
      if (nextBusy == -1 || nextBusy >= end) {
        candidates.add(r);
      }
    }

    if (candidates.isEmpty()) {
      return false;
    }

    candidates.sort(
        Comparator.comparingInt(
                (Integer r) -> bestFitTightness(roomMasks[r], start, end, horizonDays))
            .thenComparingInt(r -> totalGap(roomMasks[r], start, end, horizonDays)));

    Set<BitSet> triedSuffixes = new HashSet<>();
    for (int r : candidates) {
      BitSet suffix = roomMasks[r].get(start, horizonDays);
      if (!triedSuffixes.add(suffix)) {
        continue;
      }

      roomMasks[r].set(start, end);
      if (backtrack(intervals, index + 1, roomMasks, horizonDays, stepCounter)) {
        return true;
      }
      roomMasks[r].clear(start, end);
    }

    return false;
  }

  private static int bestFitTightness(BitSet mask, int start, int end, int horizonDays) {
    int prevBusy = start > 0 ? mask.previousSetBit(start - 1) : -1;
    int gapBefore = (prevBusy == -1) ? start : (start - 1 - prevBusy);
    int nextBusy = mask.nextSetBit(end);
    int gapAfter = (nextBusy == -1) ? (horizonDays - end) : (nextBusy - end);
    return Math.min(gapBefore, gapAfter);
  }

  private static int totalGap(BitSet mask, int start, int end, int horizonDays) {
    int prevBusy = start > 0 ? mask.previousSetBit(start - 1) : -1;
    int gapBefore = (prevBusy == -1) ? start : (start - 1 - prevBusy);
    int nextBusy = mask.nextSetBit(end);
    int gapAfter = (nextBusy == -1) ? (horizonDays - end) : (nextBusy - end);
    return gapBefore + gapAfter;
  }
}

package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class InventoryReservationDirectoryAdapter implements InventoryReservationDirectory {

  private static final int MAX_STAY_NIGHTS = 30;

  private final HotelCatalogPort hotelCatalogPort;
  private final InventoryQueryPort inventoryQueryPort;
  private final RoomPricingPort roomPricingPort;
  private final InventoryReservationJpaRepository reservationJpaRepository;
  private final Clock clock;
  private final TransactionTemplate transactionTemplate;

  public InventoryReservationDirectoryAdapter(
      HotelCatalogPort hotelCatalogPort,
      InventoryQueryPort inventoryQueryPort,
      RoomPricingPort roomPricingPort,
      InventoryReservationJpaRepository reservationJpaRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.inventoryQueryPort = inventoryQueryPort;
    this.roomPricingPort = roomPricingPort;
    this.reservationJpaRepository = reservationJpaRepository;
    this.clock = clock;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public HoldOutcome hold(HoldRequest request) {
    validateHoldRequest(request);

    return Objects.requireNonNull(
        transactionTemplate.execute(
            status -> {
              HotelCatalogPort.HotelSummary hotel =
                  hotelCatalogPort
                      .findHotelSummary(request.hotelId())
                      .orElseThrow(
                          () ->
                              InventoryException.hotelNotFound(
                                  "Khách sạn không tồn tại hoặc chưa mở bán."));

              if (!hotel.isActive() || hotel.isSuspended()) {
                throw InventoryException.hotelNotFound("Khách sạn hiện đang tạm ngừng kinh doanh.");
              }

              // Khóa hàng room_types theo thứ tự UUID tăng dần để chống deadlock và tranh phòng
              // cuối (IV-08)
              Set<UUID> sortedRoomTypeIds = new TreeSet<>();
              Map<UUID, Integer> requestedQtyByRoomType = new LinkedHashMap<>();
              for (HoldItemRequest item : request.items()) {
                sortedRoomTypeIds.add(item.roomTypeId());
                requestedQtyByRoomType.merge(item.roomTypeId(), item.quantity(), Integer::sum);
              }

              List<HotelCatalogPort.RoomTypeSummary> lockedRoomTypes =
                  hotelCatalogPort.lockActiveRoomTypesInHotel(request.hotelId(), sortedRoomTypeIds);

              if (lockedRoomTypes.size() != sortedRoomTypeIds.size()) {
                throw InventoryException.roomTypeInactive(
                    "Một hoặc nhiều loại phòng không tồn tại hoặc đã ngừng bán.");
              }

              Map<UUID, HotelCatalogPort.RoomTypeSummary> roomTypeMap =
                  lockedRoomTypes.stream()
                      .collect(
                          Collectors.toMap(
                              HotelCatalogPort.RoomTypeSummary::id, Function.identity()));

              // Lấy thời điểm now sau khi đã lấy khóa theo quy tắc AgentRules §5
              Instant now = clock.instant();
              LocalDate today = BusinessTime.today(clock);
              if (request.checkInDate().isBefore(today)) {
                throw InventoryException.invalidDateRange(
                    "Ngày nhận phòng không thể ở trong quá khứ.");
              }

              // Kiểm tra chống bấm lặp tạo đơn (idempotency theo bookingId - BK-12)
              List<InventoryReservationJpaEntity> existing =
                  reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(
                      request.bookingId());

              if (!existing.isEmpty()) {
                for (InventoryReservationJpaEntity row : existing) {
                  if (row.getStatus() == DbReservationStatus.RELEASED
                      || (row.getStatus() == DbReservationStatus.HELD
                          && (row.getExpiresAt() == null || !row.getExpiresAt().isAfter(now)))) {
                    throw InventoryException.holdExpired(
                        "Giữ chỗ của đơn đặt phòng đã hết hạn hoặc đã được giải phóng.");
                  }
                }
                return new HoldOutcome(
                    request.bookingId(), existing.get(0).getExpiresAt(), existing.size(), true);
              }

              if (!request.expiresAt().isAfter(now)) {
                throw InventoryException.holdExpired("Thời điểm hết hạn giữ chỗ phải ở tương lai.");
              }

              LocalDate lastNight = request.checkOutDate().minusDays(1);
              for (Map.Entry<UUID, Integer> entry : requestedQtyByRoomType.entrySet()) {
                UUID roomTypeId = entry.getKey();
                int requiredQuantity = entry.getValue();
                HotelCatalogPort.RoomTypeSummary rtSummary = roomTypeMap.get(roomTypeId);

                // Kiểm tra mọi đêm đều có giá hợp lệ > 0 (PR-01, PR-02, HT-03)
                Map<LocalDate, Long> rateMap =
                    roomPricingPort.getDailyRates(roomTypeId, request.checkInDate(), lastNight);
                for (LocalDate date = request.checkInDate();
                    date.isBefore(request.checkOutDate());
                    date = date.plusDays(1)) {
                  Long nightlyAmount =
                      rateMap != null && rateMap.containsKey(date)
                          ? rateMap.get(date)
                          : rtSummary.basePrice();
                  if (nightlyAmount == null || nightlyAmount <= 0L) {
                    throw InventoryException.rateNotSet(
                        "Loại phòng chưa được thiết lập giá hợp lệ cho toàn bộ kỳ lưu trú.");
                  }
                }

                // Kiểm tra khả dụng liên tục cả kỳ theo IV-04, IV-08, IV-13
                int availableQty =
                    inventoryQueryPort.calculateAvailableQuantity(
                        roomTypeId, request.checkInDate(), request.checkOutDate());
                if (availableQty < requiredQuantity) {
                  throw InventoryException.insufficientInventory(
                      "Số lượng phòng trống không đủ đáp ứng liên tục cho toàn bộ kỳ lưu trú.");
                }
              }

              List<InventoryReservationJpaEntity> toSave = new ArrayList<>();
              for (HoldItemRequest item : request.items()) {
                for (LocalDate date = request.checkInDate();
                    date.isBefore(request.checkOutDate());
                    date = date.plusDays(1)) {
                  InventoryReservationJpaEntity entity = new InventoryReservationJpaEntity();
                  entity.setBookingId(request.bookingId());
                  entity.setBookingItemId(item.bookingItemId());
                  entity.setRoomTypeId(item.roomTypeId());
                  entity.setStayDate(date);
                  entity.setQuantity((short) item.quantity());
                  entity.setStatus(DbReservationStatus.HELD);
                  entity.setExpiresAt(request.expiresAt());
                  entity.setCreatedAt(now);
                  toSave.add(entity);
                }
              }

              reservationJpaRepository.saveAll(toSave);
              return new HoldOutcome(
                  request.bookingId(), request.expiresAt(), toSave.size(), false);
            }));
  }

  @Override
  public void confirm(UUID bookingId) {
    if (bookingId == null) {
      throw InventoryException.invalidDateRange("Mã đơn đặt phòng không được để trống.");
    }

    transactionTemplate.executeWithoutResult(
        status -> {
          List<InventoryReservationJpaEntity> reservations =
              reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId);
          Instant now = clock.instant();

          if (reservations.isEmpty()) {
            throw InventoryException.holdExpired(
                "Không tìm thấy dữ liệu giữ chỗ cho đơn đặt phòng.");
          }

          boolean allConfirmed =
              reservations.stream().allMatch(r -> r.getStatus() == DbReservationStatus.CONFIRMED);
          if (allConfirmed) {
            return;
          }

          for (InventoryReservationJpaEntity row : reservations) {
            if (row.getStatus() == DbReservationStatus.RELEASED
                || (row.getStatus() == DbReservationStatus.HELD
                    && (row.getExpiresAt() == null || !row.getExpiresAt().isAfter(now)))) {
              throw InventoryException.holdExpired(
                  "Thời gian giữ chỗ đặt phòng đã hết hạn hoặc không còn hiệu lực.");
            }
          }

          for (InventoryReservationJpaEntity row : reservations) {
            if (row.getStatus() == DbReservationStatus.HELD) {
              row.setStatus(DbReservationStatus.CONFIRMED);
              row.setConfirmedAt(now);
            }
          }

          reservationJpaRepository.saveAll(reservations);
        });
  }

  @Override
  public ReleaseOutcome release(UUID bookingId, ReleaseReason reason) {
    if (bookingId == null || reason == null) {
      throw InventoryException.invalidDateRange(
          "Mã đơn đặt phòng và lý do giải phóng không được để trống.");
    }

    return Objects.requireNonNull(
        transactionTemplate.execute(
            status -> {
              List<InventoryReservationJpaEntity> reservations =
                  reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId);
              Instant now = clock.instant();

              List<InventoryReservationJpaEntity> activeRows =
                  reservations.stream()
                      .filter(
                          r ->
                              r.getStatus() == DbReservationStatus.HELD
                                  || r.getStatus() == DbReservationStatus.CONFIRMED)
                      .toList();

              if (activeRows.isEmpty()) {
                return new ReleaseOutcome(bookingId, 0, now);
              }

              for (InventoryReservationJpaEntity row : activeRows) {
                row.setStatus(DbReservationStatus.RELEASED);
                row.setReleasedAt(now);
                row.setReleaseReason(reason.name());
              }

              reservationJpaRepository.saveAll(activeRows);
              return new ReleaseOutcome(bookingId, activeRows.size(), now);
            }));
  }

  @Override
  public ReleaseOutcome releaseUnusedNights(UUID bookingId, LocalDate fromDate) {
    if (bookingId == null || fromDate == null) {
      throw InventoryException.invalidDateRange(
          "Mã đơn đặt phòng và ngày bắt đầu giải phóng không được để trống.");
    }

    return Objects.requireNonNull(
        transactionTemplate.execute(
            status -> {
              List<InventoryReservationJpaEntity> reservations =
                  reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId);
              Instant now = clock.instant();

              List<InventoryReservationJpaEntity> unusedRows =
                  reservations.stream()
                      .filter(
                          r ->
                              !r.getStayDate().isBefore(fromDate)
                                  && (r.getStatus() == DbReservationStatus.HELD
                                      || r.getStatus() == DbReservationStatus.CONFIRMED))
                      .toList();

              if (unusedRows.isEmpty()) {
                return new ReleaseOutcome(bookingId, 0, now);
              }

              for (InventoryReservationJpaEntity row : unusedRows) {
                row.setStatus(DbReservationStatus.RELEASED);
                row.setReleasedAt(now);
                row.setReleaseReason(ReleaseReason.NO_SHOW.name());
              }

              reservationJpaRepository.saveAll(unusedRows);
              return new ReleaseOutcome(bookingId, unusedRows.size(), now);
            }));
  }

  private static void validateHoldRequest(HoldRequest request) {
    if (request == null
        || request.bookingId() == null
        || request.hotelId() == null
        || request.checkInDate() == null
        || request.checkOutDate() == null
        || request.expiresAt() == null) {
      throw InventoryException.invalidDateRange("Thông tin yêu cầu giữ chỗ không được để trống.");
    }

    if (!request.checkOutDate().isAfter(request.checkInDate())) {
      throw InventoryException.invalidDateRange(
          "Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm.");
    }

    long stayDays = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
    if (stayDays > MAX_STAY_NIGHTS) {
      throw InventoryException.maxStayExceeded("Thời gian lưu trú tối đa là 30 đêm.");
    }

    if (request.items() == null || request.items().isEmpty()) {
      throw InventoryException.invalidDateRange(
          "Đơn đặt phòng phải có ít nhất một hạng mục loại phòng.");
    }

    Set<UUID> seenBookingItems = new TreeSet<>();
    for (HoldItemRequest item : request.items()) {
      if (item == null
          || item.bookingItemId() == null
          || item.roomTypeId() == null
          || item.quantity() <= 0) {
        throw InventoryException.invalidDateRange("Hạng mục giữ chỗ không hợp lệ.");
      }
      if (!seenBookingItems.add(item.bookingItemId())) {
        throw InventoryException.invalidDateRange("Hạng mục giữ chỗ bị trùng lặp bookingItemId.");
      }
    }
  }
}

package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.BookingStayOperationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.BookingStayOperationJpaEntity.DbBookingStayStatus;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.BookingStayOperationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingStayAdapter implements BookingStayPort {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final InventoryReservationJpaRepository inventoryReservationJpaRepository;
  private final BookingStayOperationJpaRepository bookingStayOperationJpaRepository;
  private final HotelCatalogPort hotelCatalogPort;
  private final BookingCheckoutDirectory bookingCheckoutDirectory;

  @Override
  public Optional<BookingStayInfo> findBookingForStayOperation(UUID hotelId, UUID bookingId) {
    List<InventoryReservationJpaEntity> reservations =
        inventoryReservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId);
    if (reservations.isEmpty()) {
      return Optional.empty();
    }

    Optional<BookingStayOperationJpaEntity> stayOpOpt =
        bookingStayOperationJpaRepository.findByBookingIdAndHotelId(bookingId, hotelId);

    boolean hasConfirmedOrPriorStay =
        stayOpOpt.isPresent()
            || reservations.stream().anyMatch(r -> r.getStatus() == DbReservationStatus.CONFIRMED);
    if (!hasConfirmedOrPriorStay) {
      return Optional.empty();
    }

    Set<UUID> hotelRoomTypeIds = new HashSet<>(hotelCatalogPort.findRoomTypeIdsByHotelId(hotelId));
    boolean belongsToHotel =
        reservations.stream().allMatch(r -> hotelRoomTypeIds.contains(r.getRoomTypeId()));
    if (!belongsToHotel) {
      return Optional.empty();
    }

    return Optional.of(buildStayInfo(hotelId, bookingId, reservations, stayOpOpt.orElse(null)));
  }

  @Override
  public BookingStayInfo recordLateArrival(
      UUID hotelId,
      UUID bookingId,
      Instant expectedArrivalAt,
      String note,
      UUID actorUserId,
      Instant recordedAt) {
    BookingStayInfo current =
        findBookingForStayOperation(hotelId, bookingId)
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng đã xác nhận trong khách sạn."));

    BookingStayOperationJpaEntity entity =
        bookingStayOperationJpaRepository
            .findByBookingIdAndHotelId(bookingId, hotelId)
            .orElseGet(
                () -> {
                  BookingStayOperationJpaEntity created = new BookingStayOperationJpaEntity();
                  created.setBookingId(bookingId);
                  created.setHotelId(hotelId);
                  created.setStayStatus(DbBookingStayStatus.CONFIRMED);
                  return created;
                });
    entity.setLateArrivalExpectedAt(expectedArrivalAt);
    entity.setLateArrivalNote(note);
    entity.setLateArrivalRecordedBy(actorUserId);
    entity.setLateArrivalRecordedAt(recordedAt);
    entity.setUpdatedAt(recordedAt);
    BookingStayOperationJpaEntity saved = bookingStayOperationJpaRepository.save(entity);

    return new BookingStayInfo(
        current.bookingId(),
        current.hotelId(),
        StayStatus.valueOf(saved.getStayStatus().name()),
        current.checkInDate(),
        current.checkOutDate(),
        current.noShowDeadlineAt(),
        saved.getLateArrivalExpectedAt(),
        saved.getLateArrivalNote(),
        current.items());
  }

  @Override
  public BookingStayInfo markCheckedIn(
      UUID hotelId, UUID bookingId, UUID actorUserId, Instant checkedInAt) {
    BookingStayInfo current =
        findBookingForStayOperation(hotelId, bookingId)
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng đã xác nhận trong khách sạn."));

    BookingStayOperationJpaEntity entity =
        bookingStayOperationJpaRepository
            .findByBookingIdAndHotelId(bookingId, hotelId)
            .orElseGet(
                () -> {
                  BookingStayOperationJpaEntity created = new BookingStayOperationJpaEntity();
                  created.setBookingId(bookingId);
                  created.setHotelId(hotelId);
                  return created;
                });
    entity.setStayStatus(DbBookingStayStatus.CHECKED_IN);
    entity.setCheckedInBy(actorUserId);
    entity.setCheckedInAt(checkedInAt);
    entity.setUpdatedAt(checkedInAt);
    BookingStayOperationJpaEntity saved = bookingStayOperationJpaRepository.save(entity);

    if (!bookingCheckoutDirectory.updateStayStatus(bookingId, "CHECKED_IN", checkedInAt)) {
      throw InventoryException.invalidStayOperation(
          "Không thể đồng bộ trạng thái nhận phòng của đơn đặt phòng.");
    }

    return new BookingStayInfo(
        current.bookingId(),
        current.hotelId(),
        StayStatus.CHECKED_IN,
        current.checkInDate(),
        current.checkOutDate(),
        current.noShowDeadlineAt(),
        saved.getLateArrivalExpectedAt(),
        saved.getLateArrivalNote(),
        current.items());
  }

  @Override
  public BookingStayInfo markCheckedOut(
      UUID hotelId, UUID bookingId, UUID actorUserId, Instant checkedOutAt) {
    BookingStayInfo current =
        findBookingForStayOperation(hotelId, bookingId)
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng đã xác nhận trong khách sạn."));

    BookingStayOperationJpaEntity entity =
        bookingStayOperationJpaRepository
            .findByBookingIdAndHotelId(bookingId, hotelId)
            .orElseGet(
                () -> {
                  BookingStayOperationJpaEntity created = new BookingStayOperationJpaEntity();
                  created.setBookingId(bookingId);
                  created.setHotelId(hotelId);
                  return created;
                });
    entity.setStayStatus(DbBookingStayStatus.CHECKED_OUT);
    entity.setCheckedOutBy(actorUserId);
    entity.setCheckedOutAt(checkedOutAt);
    entity.setUpdatedAt(checkedOutAt);
    BookingStayOperationJpaEntity saved = bookingStayOperationJpaRepository.save(entity);

    if (!bookingCheckoutDirectory.updateStayStatus(bookingId, "CHECKED_OUT", checkedOutAt)) {
      throw InventoryException.invalidStayOperation(
          "Không thể đồng bộ trạng thái trả phòng của đơn đặt phòng.");
    }

    return new BookingStayInfo(
        current.bookingId(),
        current.hotelId(),
        StayStatus.CHECKED_OUT,
        current.checkInDate(),
        current.checkOutDate(),
        current.noShowDeadlineAt(),
        saved.getLateArrivalExpectedAt(),
        saved.getLateArrivalNote(),
        current.items());
  }

  @Override
  public BookingStayInfo markNoShow(
      UUID hotelId, UUID bookingId, String reason, UUID actorUserId, Instant markedAt) {
    BookingStayInfo current =
        findBookingForStayOperation(hotelId, bookingId)
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng đã xác nhận trong khách sạn."));

    BookingStayOperationJpaEntity entity =
        bookingStayOperationJpaRepository
            .findByBookingIdAndHotelId(bookingId, hotelId)
            .orElseGet(
                () -> {
                  BookingStayOperationJpaEntity created = new BookingStayOperationJpaEntity();
                  created.setBookingId(bookingId);
                  created.setHotelId(hotelId);
                  return created;
                });
    entity.setStayStatus(DbBookingStayStatus.NO_SHOW);
    entity.setNoShowReason(reason);
    entity.setNoShowMarkedBy(actorUserId);
    entity.setNoShowMarkedAt(markedAt);
    entity.setUpdatedAt(markedAt);
    BookingStayOperationJpaEntity saved = bookingStayOperationJpaRepository.save(entity);

    if (!bookingCheckoutDirectory.updateStayStatus(bookingId, "NO_SHOW", markedAt)) {
      throw InventoryException.invalidStayOperation(
          "Không thể đồng bộ trạng thái khách không đến của đơn đặt phòng.");
    }

    return new BookingStayInfo(
        current.bookingId(),
        current.hotelId(),
        StayStatus.NO_SHOW,
        current.checkInDate(),
        current.checkOutDate(),
        current.noShowDeadlineAt(),
        saved.getLateArrivalExpectedAt(),
        saved.getLateArrivalNote(),
        current.items());
  }

  private BookingStayInfo buildStayInfo(
      UUID hotelId,
      UUID bookingId,
      List<InventoryReservationJpaEntity> reservations,
      BookingStayOperationJpaEntity stayOp) {
    Map<UUID, List<InventoryReservationJpaEntity>> byItem = new LinkedHashMap<>();
    for (InventoryReservationJpaEntity r : reservations) {
      UUID itemKey = r.getBookingItemId() != null ? r.getBookingItemId() : r.getRoomTypeId();
      byItem.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(r);
    }

    List<BookingItemStayInfo> items = new ArrayList<>();
    LocalDate overallCheckIn = null;
    LocalDate overallCheckOut = null;

    for (Map.Entry<UUID, List<InventoryReservationJpaEntity>> entry : byItem.entrySet()) {
      List<InventoryReservationJpaEntity> rows = entry.getValue();
      UUID roomTypeId = rows.getFirst().getRoomTypeId();
      int quantity = rows.stream().mapToInt(r -> r.getQuantity()).max().orElse(1);
      LocalDate itemCheckIn =
          rows.stream()
              .map(InventoryReservationJpaEntity::getStayDate)
              .min(LocalDate::compareTo)
              .orElseThrow();
      LocalDate itemCheckOut =
          rows.stream()
              .map(InventoryReservationJpaEntity::getStayDate)
              .max(LocalDate::compareTo)
              .orElseThrow()
              .plusDays(1);
      items.add(
          new BookingItemStayInfo(entry.getKey(), roomTypeId, quantity, itemCheckIn, itemCheckOut));
      if (overallCheckIn == null || itemCheckIn.isBefore(overallCheckIn)) {
        overallCheckIn = itemCheckIn;
      }
      if (overallCheckOut == null || itemCheckOut.isAfter(overallCheckOut)) {
        overallCheckOut = itemCheckOut;
      }
    }

    StayStatus status =
        stayOp != null ? StayStatus.valueOf(stayOp.getStayStatus().name()) : StayStatus.CONFIRMED;
    Instant noShowDeadlineAt = overallCheckIn.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    Instant lateArrivalExpectedAt = stayOp != null ? stayOp.getLateArrivalExpectedAt() : null;
    String lateArrivalNote = stayOp != null ? stayOp.getLateArrivalNote() : null;

    return new BookingStayInfo(
        bookingId,
        hotelId,
        status,
        overallCheckIn,
        overallCheckOut,
        noShowDeadlineAt,
        lateArrivalExpectedAt,
        lateArrivalNote,
        items);
  }
}

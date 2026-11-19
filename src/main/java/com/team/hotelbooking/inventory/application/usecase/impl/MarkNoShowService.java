package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.application.command.MarkNoShowCommand;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.usecase.MarkNoShowUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MarkNoShowService implements MarkNoShowUseCase {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final HotelCatalogPort hotelCatalogPort;
  private final BookingStayPort bookingStayPort;
  private final RoomAssignmentRepository roomAssignmentRepository;
  private final InventoryReservationDirectory inventoryReservationDirectory;
  private final Clock clock;

  public MarkNoShowService(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      InventoryReservationDirectory inventoryReservationDirectory,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.bookingStayPort = bookingStayPort;
    this.roomAssignmentRepository = roomAssignmentRepository;
    this.inventoryReservationDirectory = inventoryReservationDirectory;
    this.clock = clock;
  }

  @Override
  public BookingStayOperationResult execute(MarkNoShowCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.bookingId() == null) {
      throw InventoryException.invalidStayOperation(
          "Thông tin đánh dấu khách không đến (NO_SHOW) không được để trống.");
    }

    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    BookingStayInfo booking =
        bookingStayPort
            .findBookingForStayOperation(command.hotelId(), command.bookingId())
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng trong khách sạn."));

    List<UUID> roomTypeIds =
        booking.items().stream().map(BookingItemStayInfo::roomTypeId).distinct().sorted().toList();
    hotelCatalogPort.lockRoomTypesInHotel(command.hotelId(), roomTypeIds);

    Instant now = clock.instant();
    LocalDate todayBusinessDate = LocalDate.ofInstant(now, BUSINESS_ZONE);

    // Idempotent: nếu đã ở trạng thái NO_SHOW thì không trả tồn kho lần hai (IV-07, IV-11)
    if (booking.status() == StayStatus.NO_SHOW) {
      List<RoomAssignmentResult> history =
          roomAssignmentRepository.findAllByBookingId(command.bookingId()).stream()
              .map(RoomAssignmentResult::from)
              .toList();
      return new BookingStayOperationResult(
          command.hotelId(),
          command.bookingId(),
          booking.status().name(),
          booking.checkInDate(),
          booking.checkOutDate(),
          booking.lateArrivalExpectedAt(),
          booking.lateArrivalNote(),
          0,
          now,
          history);
    }

    if (booking.status() != StayStatus.CONFIRMED) {
      throw InventoryException.invalidStayOperation(
          "Chỉ được đánh dấu NO_SHOW cho đơn đặt phòng ở trạng thái CONFIRMED và chưa nhận phòng (BK-10).");
    }

    if (booking.noShowDeadlineAt() != null && now.isBefore(booking.noShowDeadlineAt())) {
      throw InventoryException.invalidStayOperation(
          "Chưa quá thời điểm giới hạn nhận phòng tiêu chuẩn để đánh dấu NO_SHOW (BK-10).");
    }

    if (booking.lateArrivalExpectedAt() != null && now.isBefore(booking.lateArrivalExpectedAt())) {
      throw InventoryException.invalidStayOperation(
          "Đơn đặt phòng đang có thông báo khách đến muộn còn hiệu lực, chưa thể đánh dấu NO_SHOW (BK-09, BK-10).");
    }

    String noShowReason =
        command.reason() != null && !command.reason().trim().isEmpty()
            ? command.reason().trim()
            : "Khách không đến nhận phòng (NO_SHOW)";

    // BK-10: Giải phóng các phân công phòng đang hiệu lực mà KHÔNG chuyển phòng sang DIRTY
    List<RoomAssignment> activeAssignments =
        roomAssignmentRepository.findActiveByBookingId(command.bookingId());
    List<RoomAssignment> releasedAssignments = new ArrayList<>(activeAssignments.size());
    for (RoomAssignment a : activeAssignments) {
      releasedAssignments.add(
          new RoomAssignment(
              a.id(),
              a.hotelId(),
              a.bookingId(),
              a.bookingItemId(),
              a.roomTypeId(),
              a.roomId(),
              a.roomNumberSnapshot(),
              a.startDate(),
              a.endDate(),
              RoomAssignment.Status.RELEASED,
              a.assignedByUserId(),
              a.assignedAt(),
              a.checkedInByUserId(),
              a.checkedInAt(),
              a.checkedOutByUserId(),
              a.checkedOutAt(),
              command.actorId(),
              now,
              noShowReason));
    }
    if (!releasedAssignments.isEmpty()) {
      roomAssignmentRepository.saveAll(releasedAssignments);
    }

    // IV-11, BK-14: Giải phóng tồn kho của các đêm chưa sử dụng từ ngày hiện tại trở đi, giữ nguyên
    // đêm đã qua
    InventoryReservationDirectory.ReleaseOutcome releaseResult =
        inventoryReservationDirectory.releaseUnusedNights(command.bookingId(), todayBusinessDate);

    BookingStayInfo updatedStay =
        bookingStayPort.markNoShow(
            command.hotelId(), command.bookingId(), noShowReason, command.actorId(), now);

    List<RoomAssignmentResult> allAssignments =
        roomAssignmentRepository.findAllByBookingId(command.bookingId()).stream()
            .map(RoomAssignmentResult::from)
            .toList();

    return new BookingStayOperationResult(
        command.hotelId(),
        command.bookingId(),
        updatedStay.status().name(),
        updatedStay.checkInDate(),
        updatedStay.checkOutDate(),
        updatedStay.lateArrivalExpectedAt(),
        updatedStay.lateArrivalNote(),
        releaseResult.releasedRowCount(),
        now,
        allAssignments);
  }
}

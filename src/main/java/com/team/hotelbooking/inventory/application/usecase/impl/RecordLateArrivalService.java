package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.RecordLateArrivalCommand;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.usecase.RecordLateArrivalUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class RecordLateArrivalService implements RecordLateArrivalUseCase {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final HotelCatalogPort hotelCatalogPort;
  private final BookingStayPort bookingStayPort;
  private final RoomAssignmentRepository roomAssignmentRepository;
  private final Clock clock;

  public RecordLateArrivalService(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      Clock clock) {
    this.hotelCatalogPort = hotelCatalogPort;
    this.bookingStayPort = bookingStayPort;
    this.roomAssignmentRepository = roomAssignmentRepository;
    this.clock = clock;
  }

  @Override
  public BookingStayOperationResult execute(RecordLateArrivalCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.bookingId() == null
        || command.expectedArrivalAt() == null) {
      throw InventoryException.invalidStayOperation(
          "Thông tin đơn đặt phòng và thời điểm dự kiến khách đến muộn không được để trống (BK-09).");
    }

    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    BookingStayInfo booking =
        bookingStayPort
            .findBookingForStayOperation(command.hotelId(), command.bookingId())
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng trong khách sạn."));

    if (booking.status() != StayStatus.CONFIRMED) {
      throw InventoryException.invalidStayOperation(
          "Chỉ được ghi nhận lịch đến muộn cho đơn đặt phòng ở trạng thái CONFIRMED chưa check-in và chưa NO_SHOW (BK-09).");
    }

    List<UUID> roomTypeIds =
        booking.items().stream().map(BookingItemStayInfo::roomTypeId).distinct().sorted().toList();
    hotelCatalogPort.lockRoomTypesInHotel(command.hotelId(), roomTypeIds);

    Instant now = clock.instant();
    if (!command.expectedArrivalAt().isAfter(now)) {
      throw InventoryException.invalidStayOperation(
          "Thời điểm dự kiến khách đến muộn phải sau thời điểm hiện tại (BK-09).");
    }

    Instant checkOutBoundary = booking.checkOutDate().atStartOfDay(BUSINESS_ZONE).toInstant();
    if (!command.expectedArrivalAt().isBefore(checkOutBoundary)) {
      throw InventoryException.invalidStayOperation(
          "Thời điểm dự kiến khách đến muộn không được vượt quá ngày trả phòng của đơn (BK-09).");
    }

    BookingStayInfo updated =
        bookingStayPort.recordLateArrival(
            command.hotelId(),
            command.bookingId(),
            command.expectedArrivalAt(),
            command.note() != null ? command.note().trim() : null,
            command.actorId(),
            now);

    List<RoomAssignmentResult> assignments =
        roomAssignmentRepository.findActiveByBookingId(command.bookingId()).stream()
            .map(RoomAssignmentResult::from)
            .toList();

    return new BookingStayOperationResult(
        command.hotelId(),
        command.bookingId(),
        updated.status().name(),
        updated.checkInDate(),
        updated.checkOutDate(),
        updated.lateArrivalExpectedAt(),
        updated.lateArrivalNote(),
        0,
        now,
        assignments);
  }
}

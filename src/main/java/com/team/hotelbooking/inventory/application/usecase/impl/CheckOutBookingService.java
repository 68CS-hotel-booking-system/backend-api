package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.CheckOutBookingCommand;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.usecase.CheckOutBookingUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckOutBookingService implements CheckOutBookingUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final BookingStayPort bookingStayPort;
  private final RoomAssignmentRepository roomAssignmentRepository;
  private final Clock clock;

  public CheckOutBookingService(
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
  public BookingStayOperationResult execute(CheckOutBookingCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.bookingId() == null) {
      throw InventoryException.invalidStayOperation("Thông tin trả phòng không được để trống.");
    }

    hotelCatalogPort.requireManagementAccess(command.actorId(), command.hotelId());

    BookingStayInfo booking =
        bookingStayPort
            .findBookingForStayOperation(command.hotelId(), command.bookingId())
            .orElseThrow(
                () ->
                    InventoryException.bookingNotFound(
                        "Không tìm thấy đơn đặt phòng trong khách sạn."));

    if (booking.status() != StayStatus.CHECKED_IN) {
      throw InventoryException.invalidStayOperation(
          "Chỉ được làm thủ tục trả phòng (check-out) khi khách đã nhận phòng (CHECKED_IN) (BK-18).");
    }

    List<UUID> roomTypeIds =
        booking.items().stream().map(BookingItemStayInfo::roomTypeId).distinct().sorted().toList();
    hotelCatalogPort.lockRoomTypesInHotel(command.hotelId(), roomTypeIds);

    Instant now = clock.instant();

    List<RoomAssignment> activeAssignments =
        roomAssignmentRepository.findActiveByBookingId(command.bookingId());

    List<RoomAssignment> checkedOutAssignments = new ArrayList<>(activeAssignments.size());
    for (RoomAssignment a : activeAssignments) {
      checkedOutAssignments.add(
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
              RoomAssignment.Status.CHECKED_OUT,
              a.assignedByUserId(),
              a.assignedAt(),
              a.checkedInByUserId(),
              a.checkedInAt(),
              command.actorId(),
              now,
              null,
              null,
              null));
      // BK-18: Chuyển trạng thái buồng phòng của mọi phòng đã phân sang DIRTY để chờ dọn phòng
      hotelCatalogPort.updateRoomReadiness(
          a.roomId(), command.hotelId(), "DIRTY", command.actorId(), now);
    }

    List<RoomAssignment> savedAssignments = roomAssignmentRepository.saveAll(checkedOutAssignments);

    // IV-10: Khách trả phòng sớm hơn ngày trả phòng đã đặt KHÔNG tự động mở bán lại các đêm còn lại
    BookingStayInfo updatedStay =
        bookingStayPort.markCheckedOut(
            command.hotelId(), command.bookingId(), command.actorId(), now);

    return new BookingStayOperationResult(
        command.hotelId(),
        command.bookingId(),
        updatedStay.status().name(),
        updatedStay.checkInDate(),
        updatedStay.checkOutDate(),
        updatedStay.lateArrivalExpectedAt(),
        updatedStay.lateArrivalNote(),
        0,
        now,
        savedAssignments.stream().map(RoomAssignmentResult::from).toList());
  }
}

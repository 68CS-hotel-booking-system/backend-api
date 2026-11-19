package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.CheckInBookingCommand;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.PhysicalRoomSummary;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.usecase.CheckInBookingUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CheckInBookingService implements CheckInBookingUseCase {

  private final HotelCatalogPort hotelCatalogPort;
  private final BookingStayPort bookingStayPort;
  private final RoomAssignmentRepository roomAssignmentRepository;
  private final Clock clock;

  public CheckInBookingService(
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
  public BookingStayOperationResult execute(CheckInBookingCommand command) {
    if (command == null
        || command.actorId() == null
        || command.hotelId() == null
        || command.bookingId() == null) {
      throw InventoryException.invalidStayOperation("Thông tin nhận phòng không được để trống.");
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
          "Chỉ được làm thủ tục nhận phòng (check-in) khi đơn đặt phòng ở trạng thái CONFIRMED và chưa bị đánh dấu NO_SHOW (BK-08).");
    }

    List<UUID> roomTypeIds =
        booking.items().stream().map(BookingItemStayInfo::roomTypeId).distinct().sorted().toList();
    hotelCatalogPort.lockRoomTypesInHotel(command.hotelId(), roomTypeIds);

    Instant now = clock.instant();
    LocalDate today = BusinessTime.today(clock);

    if (today.isBefore(booking.checkInDate())) {
      throw InventoryException.invalidStayOperation(
          "Chưa đến ngày nhận phòng của đơn đặt phòng (BK-08).");
    }

    if (!today.isBefore(booking.checkOutDate())) {
      throw InventoryException.invalidStayOperation(
          "Đã qua ngày trả phòng của đơn đặt phòng, không thể làm thủ tục nhận phòng (BK-08).");
    }

    if (booking.noShowDeadlineAt() != null && !now.isBefore(booking.noShowDeadlineAt())) {
      if (booking.lateArrivalExpectedAt() == null
          || !now.isBefore(booking.lateArrivalExpectedAt())) {
        throw InventoryException.invalidStayOperation(
            "Đã quá thời hạn nhận phòng tiêu chuẩn và không có thông báo đến muộn còn hiệu lực (BK-08).");
      }
    }

    List<RoomAssignment> activeAssignments =
        roomAssignmentRepository.findActiveByBookingId(command.bookingId());

    Map<UUID, Long> assignedCountByItem =
        activeAssignments.stream()
            .collect(Collectors.groupingBy(RoomAssignment::bookingItemId, Collectors.counting()));

    for (BookingItemStayInfo item : booking.items()) {
      long assignedCount = assignedCountByItem.getOrDefault(item.bookingItemId(), 0L);
      if (assignedCount != item.quantity()) {
        throw InventoryException.invalidStayOperation(
            "Đơn đặt phòng chưa được phân đủ số lượng phòng vật lý cho mọi hạng mục (BK-08).");
      }
    }

    for (RoomAssignment assignment : activeAssignments) {
      PhysicalRoomSummary room =
          hotelCatalogPort
              .findPhysicalRoomByIdAndHotelId(assignment.roomId(), command.hotelId())
              .orElseThrow(
                  () ->
                      InventoryException.roomNotFound("Không tìm thấy phòng vật lý đã phân công."));
      if (!"READY".equalsIgnoreCase(room.readiness())) {
        throw InventoryException.roomNotReady(
            "Phòng vật lý "
                + room.roomNumber()
                + " chưa ở trạng thái sẵn sàng (READY), không thể làm thủ tục nhận phòng (BK-08).");
      }
    }

    List<RoomAssignment> updatedAssignments = new ArrayList<>(activeAssignments.size());
    for (RoomAssignment a : activeAssignments) {
      updatedAssignments.add(
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
              RoomAssignment.Status.CHECKED_IN,
              a.assignedByUserId(),
              a.assignedAt(),
              command.actorId(),
              now,
              null,
              null,
              null,
              null,
              null));
    }

    List<RoomAssignment> savedAssignments = roomAssignmentRepository.saveAll(updatedAssignments);
    BookingStayInfo updatedStay =
        bookingStayPort.markCheckedIn(
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

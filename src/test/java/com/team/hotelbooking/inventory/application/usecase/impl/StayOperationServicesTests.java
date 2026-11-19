package com.team.hotelbooking.inventory.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.ReleaseOutcome;
import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand;
import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand.RoomAssignmentItem;
import com.team.hotelbooking.inventory.application.command.CheckInBookingCommand;
import com.team.hotelbooking.inventory.application.command.CheckOutBookingCommand;
import com.team.hotelbooking.inventory.application.command.MarkNoShowCommand;
import com.team.hotelbooking.inventory.application.command.RecordLateArrivalCommand;
import com.team.hotelbooking.inventory.application.command.UpdateRoomReadinessCommand;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingItemStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.BookingStayInfo;
import com.team.hotelbooking.inventory.application.port.BookingStayPort.StayStatus;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.HotelSummary;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.PhysicalRoomSummary;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.RoomTypeSummary;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ phân phòng vật lý và vận hành lưu trú (BK-08..10, BK-14..18, IV-10..11)")
class StayOperationServicesTests {

  @Mock private HotelCatalogPort hotelCatalogPort;
  @Mock private InventoryQueryPort inventoryQueryPort;
  @Mock private BookingStayPort bookingStayPort;
  @Mock private RoomAssignmentRepository roomAssignmentRepository;
  @Mock private RoomBlockRepository roomBlockRepository;
  @Mock private InventoryReservationDirectory inventoryReservationDirectory;

  private final Instant fixedNow = Instant.parse("2026-11-10T18:00:00Z"); // 2026-11-11 01:00 ICT
  private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

  private final UUID actorId = UUID.randomUUID();
  private final UUID hotelId = UUID.randomUUID();
  private final UUID bookingId = UUID.randomUUID();
  private final UUID bookingItemId = UUID.randomUUID();
  private final UUID roomTypeId = UUID.randomUUID();
  private final UUID room301Id = UUID.randomUUID();
  private final UUID room302Id = UUID.randomUUID();

  private AssignRoomsService assignRoomsService;
  private CheckInBookingService checkInBookingService;
  private CheckOutBookingService checkOutBookingService;
  private RecordLateArrivalService recordLateArrivalService;
  private MarkNoShowService markNoShowService;
  private UpdateRoomReadinessService updateRoomReadinessService;

  @BeforeEach
  void setUp() {
    assignRoomsService =
        new AssignRoomsService(
            hotelCatalogPort,
            bookingStayPort,
            roomAssignmentRepository,
            roomBlockRepository,
            inventoryQueryPort,
            fixedClock);
    checkInBookingService =
        new CheckInBookingService(
            hotelCatalogPort, bookingStayPort, roomAssignmentRepository, fixedClock);
    checkOutBookingService =
        new CheckOutBookingService(
            hotelCatalogPort, bookingStayPort, roomAssignmentRepository, fixedClock);
    recordLateArrivalService =
        new RecordLateArrivalService(
            hotelCatalogPort, bookingStayPort, roomAssignmentRepository, fixedClock);
    markNoShowService =
        new MarkNoShowService(
            hotelCatalogPort,
            bookingStayPort,
            roomAssignmentRepository,
            inventoryReservationDirectory,
            fixedClock);
    updateRoomReadinessService = new UpdateRoomReadinessService(hotelCatalogPort, fixedClock);
  }

  @Nested
  @DisplayName("AssignRoomsService (BK-15, BK-16, BK-17)")
  class AssignRoomsTests {

    @Test
    @DisplayName(
        "Đổi phân công phòng vật lý giữ lại bản ghi cũ ở trạng thái RELEASED kèm lý do (BK-16)")
    void shouldPreserveHistoryWhenReassigningRoom() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 12);
      BookingStayInfo booking =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CONFIRMED,
              checkIn,
              checkOut,
              Instant.parse("2026-11-10T17:00:00Z"),
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      PhysicalRoomSummary room301 =
          new PhysicalRoomSummary(
              room301Id,
              hotelId,
              roomTypeId,
              "301",
              "3",
              "READY",
              Instant.parse("2026-01-01T00:00:00Z"),
              null);
      PhysicalRoomSummary room302 =
          new PhysicalRoomSummary(
              room302Id,
              hotelId,
              roomTypeId,
              "302",
              "3",
              "READY",
              Instant.parse("2026-01-01T00:00:00Z"),
              null);

      RoomAssignment previousAssignment =
          new RoomAssignment(
              UUID.randomUUID(),
              hotelId,
              bookingId,
              bookingItemId,
              roomTypeId,
              room301Id,
              "301",
              checkIn,
              checkOut,
              RoomAssignment.Status.ASSIGNED,
              actorId,
              Instant.parse("2026-11-09T10:00:00Z"),
              null,
              null,
              null,
              null,
              null,
              null,
              null);

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(booking));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(hotelCatalogPort.findPhysicalRoomsByHotelId(hotelId))
          .willReturn(List.of(room301, room302));
      given(
              roomBlockRepository.findActiveBlocksByRoomIdInRange(
                  roomTypeId, room302Id, checkIn, checkOut.minusDays(1)))
          .willReturn(List.of());
      given(roomAssignmentRepository.findActiveByRoomIdOverlapping(room302Id, checkIn, checkOut))
          .willReturn(List.of());
      given(roomAssignmentRepository.findActiveByBookingId(bookingId))
          .willReturn(List.of(previousAssignment));

      AssignRoomsCommand command =
          new AssignRoomsCommand(
              actorId,
              hotelId,
              bookingId,
              List.of(new RoomAssignmentItem(bookingItemId, List.of(room302Id))),
              "Khách đổi sang phòng 302");

      given(inventoryQueryPort.canAllocateAfterAssignment(roomTypeId, checkIn, checkOut))
          .willReturn(true);

      BookingRoomAssignmentsResult result = assignRoomsService.execute(command);

      assertThat(result).isNotNull();
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<RoomAssignment>> captor = ArgumentCaptor.forClass(List.class);
      then(roomAssignmentRepository).should().saveAll(captor.capture());

      List<RoomAssignment> saved = captor.getValue();
      assertThat(saved).hasSize(2);
      assertThat(saved)
          .anySatisfy(
              a -> {
                assertThat(a.roomId()).isEqualTo(room301Id);
                assertThat(a.status()).isEqualTo(RoomAssignment.Status.RELEASED);
                assertThat(a.releaseReason()).isEqualTo("Khách đổi sang phòng 302");
              })
          .anySatisfy(
              a -> {
                assertThat(a.roomId()).isEqualTo(room302Id);
                assertThat(a.roomNumberSnapshot()).isEqualTo("302");
                assertThat(a.status()).isEqualTo(RoomAssignment.Status.ASSIGNED);
              });
    }

    @Test
    @DisplayName("Từ chối khi phòng vật lý đã được phân cho đơn đặt phòng khác chồng lịch (BK-15)")
    void shouldRejectWhenRoomAlreadyAssignedToOverlappingBooking() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 12);
      BookingStayInfo booking =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CONFIRMED,
              checkIn,
              checkOut,
              Instant.parse("2026-11-10T17:00:00Z"),
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      PhysicalRoomSummary room301 =
          new PhysicalRoomSummary(
              room301Id,
              hotelId,
              roomTypeId,
              "301",
              "3",
              "READY",
              Instant.parse("2026-01-01T00:00:00Z"),
              null);

      RoomAssignment otherBookingAssignment =
          new RoomAssignment(
              UUID.randomUUID(),
              hotelId,
              UUID.randomUUID(),
              UUID.randomUUID(),
              roomTypeId,
              room301Id,
              "301",
              checkIn,
              checkOut,
              RoomAssignment.Status.ASSIGNED,
              actorId,
              fixedNow,
              null,
              null,
              null,
              null,
              null,
              null,
              null);

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(booking));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(hotelCatalogPort.findPhysicalRoomsByHotelId(hotelId)).willReturn(List.of(room301));
      given(
              roomBlockRepository.findActiveBlocksByRoomIdInRange(
                  roomTypeId, room301Id, checkIn, checkOut.minusDays(1)))
          .willReturn(List.of());
      given(roomAssignmentRepository.findActiveByRoomIdOverlapping(room301Id, checkIn, checkOut))
          .willReturn(List.of(otherBookingAssignment));

      AssignRoomsCommand command =
          new AssignRoomsCommand(
              actorId,
              hotelId,
              bookingId,
              List.of(new RoomAssignmentItem(bookingItemId, List.of(room301Id))),
              null);

      assertThatThrownBy(() -> assignRoomsService.execute(command))
          .isInstanceOf(InventoryException.class)
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.ROOM_ASSIGNMENT_CONFLICT));
    }
  }

  @Nested
  @DisplayName("CheckInBookingService & CheckOutBookingService (BK-08, BK-18, IV-10)")
  class CheckInOutTests {

    @Test
    @DisplayName("Chặn check-in khi phòng vật lý đã phân ở trạng thái DIRTY (BK-08)")
    void shouldRejectCheckInWhenAssignedRoomIsDirty() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 12);
      BookingStayInfo booking =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CONFIRMED,
              checkIn,
              checkOut,
              Instant.parse("2026-11-11T00:00:00Z"),
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      RoomAssignment assignment =
          new RoomAssignment(
              UUID.randomUUID(),
              hotelId,
              bookingId,
              bookingItemId,
              roomTypeId,
              room301Id,
              "301",
              checkIn,
              checkOut,
              RoomAssignment.Status.ASSIGNED,
              actorId,
              fixedNow,
              null,
              null,
              null,
              null,
              null,
              null,
              null);

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(booking));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(roomAssignmentRepository.findActiveByBookingId(bookingId))
          .willReturn(List.of(assignment));
      given(hotelCatalogPort.findPhysicalRoomByIdAndHotelId(room301Id, hotelId))
          .willReturn(
              Optional.of(
                  new PhysicalRoomSummary(
                      room301Id,
                      hotelId,
                      roomTypeId,
                      "301",
                      "3",
                      "DIRTY",
                      Instant.parse("2026-01-01T00:00:00Z"),
                      null)));

      assertThatThrownBy(
              () ->
                  checkInBookingService.execute(
                      new CheckInBookingCommand(actorId, hotelId, bookingId)))
          .isInstanceOf(InventoryException.class)
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.ROOM_NOT_READY));
    }

    @Test
    @DisplayName(
        "Check-out chuyển phòng sang DIRTY và KHÔNG giải phóng tồn kho đêm còn lại nếu trả sớm (BK-18, IV-10)")
    void shouldMarkRoomDirtyAndNotReleaseRemainingNightsOnEarlyCheckOut() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 15);
      BookingStayInfo checkedInBooking =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CHECKED_IN,
              checkIn,
              checkOut,
              Instant.parse("2026-11-11T00:00:00Z"),
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      RoomAssignment activeAssignment =
          new RoomAssignment(
              UUID.randomUUID(),
              hotelId,
              bookingId,
              bookingItemId,
              roomTypeId,
              room301Id,
              "301",
              checkIn,
              checkOut,
              RoomAssignment.Status.CHECKED_IN,
              actorId,
              fixedNow,
              actorId,
              fixedNow,
              null,
              null,
              null,
              null,
              null);

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(checkedInBooking));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(roomAssignmentRepository.findActiveByBookingId(bookingId))
          .willReturn(List.of(activeAssignment));
      given(roomAssignmentRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));
      given(bookingStayPort.markCheckedOut(hotelId, bookingId, actorId, fixedNow))
          .willReturn(
              new BookingStayInfo(
                  bookingId,
                  hotelId,
                  StayStatus.CHECKED_OUT,
                  checkIn,
                  checkOut,
                  Instant.parse("2026-11-11T00:00:00Z"),
                  null,
                  null,
                  checkedInBooking.items()));

      BookingStayOperationResult result =
          checkOutBookingService.execute(new CheckOutBookingCommand(actorId, hotelId, bookingId));

      assertThat(result.stayStatus()).isEqualTo("CHECKED_OUT");
      assertThat(result.releasedRoomNights()).isZero();
      then(hotelCatalogPort)
          .should()
          .updateRoomReadiness(room301Id, hotelId, "DIRTY", actorId, fixedNow);
      then(inventoryReservationDirectory).should(never()).releaseUnusedNights(any(), any());
    }
  }

  @Nested
  @DisplayName("MarkNoShowService & RecordLateArrivalService (BK-09, BK-10, BK-14, IV-11)")
  class NoShowAndLateArrivalTests {

    @Test
    @DisplayName(
        "MarkNoShow chỉ giải phóng các đêm chưa sử dụng từ ngày hiện tại trở đi và không chuyển phòng sang DIRTY (BK-10, IV-11)")
    void shouldReleaseOnlyUnusedNightsFromTodayAndNotDirtyRoom() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 13);
      BookingStayInfo booking =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CONFIRMED,
              checkIn,
              checkOut,
              Instant.parse("2026-11-10T17:00:00Z"), // 00:00 ngày 11/11 ICT (đã qua lúc 01:00 ICT)
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      RoomAssignment assigned =
          new RoomAssignment(
              UUID.randomUUID(),
              hotelId,
              bookingId,
              bookingItemId,
              roomTypeId,
              room301Id,
              "301",
              checkIn,
              checkOut,
              RoomAssignment.Status.ASSIGNED,
              actorId,
              Instant.parse("2026-11-10T07:00:00Z"),
              null,
              null,
              null,
              null,
              null,
              null,
              null);

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(booking));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(roomAssignmentRepository.findActiveByBookingId(bookingId))
          .willReturn(List.of(assigned));
      given(
              inventoryReservationDirectory.releaseUnusedNights(
                  bookingId, LocalDate.of(2026, 11, 11)))
          .willReturn(new ReleaseOutcome(bookingId, 2, fixedNow));
      given(
              bookingStayPort.markNoShow(
                  eq(hotelId), eq(bookingId), any(), eq(actorId), eq(fixedNow)))
          .willReturn(
              new BookingStayInfo(
                  bookingId,
                  hotelId,
                  StayStatus.NO_SHOW,
                  checkIn,
                  checkOut,
                  booking.noShowDeadlineAt(),
                  null,
                  null,
                  booking.items()));

      BookingStayOperationResult result =
          markNoShowService.execute(
              new MarkNoShowCommand(actorId, hotelId, bookingId, "Khách không đến"));

      assertThat(result.stayStatus()).isEqualTo("NO_SHOW");
      assertThat(result.releasedRoomNights()).isEqualTo(2);
      then(hotelCatalogPort).should(never()).updateRoomReadiness(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "Gọi MarkNoShow lặp lại trên đơn đã NO_SHOW là idempotent, không trả tồn lần hai (IV-07)")
    void shouldBeIdempotentWhenAlreadyNoShow() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 13);
      BookingStayInfo alreadyNoShow =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.NO_SHOW,
              checkIn,
              checkOut,
              Instant.parse("2026-11-10T17:00:00Z"),
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(alreadyNoShow));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(roomAssignmentRepository.findAllByBookingId(bookingId)).willReturn(List.of());

      BookingStayOperationResult result =
          markNoShowService.execute(new MarkNoShowCommand(actorId, hotelId, bookingId, null));

      assertThat(result.stayStatus()).isEqualTo("NO_SHOW");
      assertThat(result.releasedRoomNights()).isZero();
      then(inventoryReservationDirectory).should(never()).releaseUnusedNights(any(), any());
    }

    @Test
    @DisplayName("Chặn MarkNoShow khi khách có thông báo đến muộn còn hiệu lực (BK-09, BK-10)")
    void shouldRejectNoShowWhenValidLateArrivalExists() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 13);
      BookingStayInfo bookingWithLateArrival =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CONFIRMED,
              checkIn,
              checkOut,
              Instant.parse("2026-11-10T17:00:00Z"),
              Instant.parse("2026-11-10T22:00:00Z"), // còn hiệu lực sau fixedNow (18:00Z)
              "Chuyến bay trễ",
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(bookingWithLateArrival));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));

      assertThatThrownBy(
              () ->
                  markNoShowService.execute(
                      new MarkNoShowCommand(actorId, hotelId, bookingId, null)))
          .isInstanceOf(InventoryException.class)
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.INVALID_STAY_OPERATION));
    }

    @Test
    @DisplayName("Ghi nhận lịch đến muộn thành công (BK-09)")
    void shouldRecordLateArrivalSuccessfully() {
      LocalDate checkIn = LocalDate.of(2026, 11, 10);
      LocalDate checkOut = LocalDate.of(2026, 11, 13);
      Instant expectedArrival = Instant.parse("2026-11-10T23:00:00Z");
      BookingStayInfo booking =
          new BookingStayInfo(
              bookingId,
              hotelId,
              StayStatus.CONFIRMED,
              checkIn,
              checkOut,
              Instant.parse("2026-11-10T17:00:00Z"),
              null,
              null,
              List.of(new BookingItemStayInfo(bookingItemId, roomTypeId, 1, checkIn, checkOut)));

      given(bookingStayPort.findBookingForStayOperation(hotelId, bookingId))
          .willReturn(Optional.of(booking));
      given(hotelCatalogPort.lockRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(List.of(new RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(
              bookingStayPort.recordLateArrival(
                  hotelId, bookingId, expectedArrival, "Khách báo đến trễ", actorId, fixedNow))
          .willReturn(
              new BookingStayInfo(
                  bookingId,
                  hotelId,
                  StayStatus.CONFIRMED,
                  checkIn,
                  checkOut,
                  booking.noShowDeadlineAt(),
                  expectedArrival,
                  "Khách báo đến trễ",
                  booking.items()));
      given(roomAssignmentRepository.findActiveByBookingId(bookingId)).willReturn(List.of());

      BookingStayOperationResult result =
          recordLateArrivalService.execute(
              new RecordLateArrivalCommand(
                  actorId, hotelId, bookingId, expectedArrival, "Khách báo đến trễ"));

      assertThat(result.lateArrivalExpectedAt()).isEqualTo(expectedArrival);
      assertThat(result.lateArrivalNote()).isEqualTo("Khách báo đến trễ");
    }
  }

  @Nested
  @DisplayName("UpdateRoomReadinessService")
  class UpdateRoomReadinessTests {

    @Test
    @DisplayName("Cập nhật trạng thái buồng phòng sang READY thành công")
    void shouldUpdateRoomReadinessSuccessfully() {
      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.updateRoomReadiness(room301Id, hotelId, "READY", actorId, fixedNow))
          .willReturn(
              Optional.of(
                  new PhysicalRoomSummary(
                      room301Id,
                      hotelId,
                      roomTypeId,
                      "301",
                      "3",
                      "READY",
                      Instant.parse("2026-01-01T00:00:00Z"),
                      null)));

      RoomReadinessResult result =
          updateRoomReadinessService.execute(
              new UpdateRoomReadinessCommand(actorId, hotelId, room301Id, "ready"));

      assertThat(result.readiness()).isEqualTo("READY");
      assertThat(result.roomNumber()).isEqualTo("301");
    }
  }
}

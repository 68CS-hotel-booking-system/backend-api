package com.team.hotelbooking.inventory.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team.hotelbooking.inventory.application.command.CreateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.command.GetRoomCalendarQuery;
import com.team.hotelbooking.inventory.application.command.ReleaseRoomBlockCommand;
import com.team.hotelbooking.inventory.application.command.UpdateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.RoomBlock;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ quản lý lịch chặn phòng và lịch phòng (BL-01..12)")
class RoomBlockServicesTests {

  @Mock private HotelCatalogPort hotelCatalogPort;
  @Mock private RoomBlockRepository roomBlockRepository;
  @Mock private InventoryQueryPort inventoryQueryPort;

  private Clock clock;
  private Instant now;
  private LocalDate today;

  private CreateRoomBlockService createService;
  private UpdateRoomBlockService updateService;
  private ReleaseRoomBlockService releaseService;
  private GetRoomCalendarService calendarService;

  @BeforeEach
  void setUp() {
    now = Instant.parse("2026-11-10T09:00:00Z");
    clock = Clock.fixed(now, ZoneOffset.UTC);
    today = LocalDate.now(clock);

    createService =
        new CreateRoomBlockService(
            hotelCatalogPort, roomBlockRepository, inventoryQueryPort, clock);
    updateService =
        new UpdateRoomBlockService(
            hotelCatalogPort, roomBlockRepository, inventoryQueryPort, clock);
    releaseService = new ReleaseRoomBlockService(roomBlockRepository, clock);
    calendarService =
        new GetRoomCalendarService(
            hotelCatalogPort, roomBlockRepository, inventoryQueryPort, clock);
  }

  @Nested
  @DisplayName("CreateRoomBlockUseCase.execute")
  class CreateRoomBlockTests {

    @Test
    @DisplayName(
        "Tạo lịch chặn ACTIVE thành công khi phòng hợp lệ và không làm mất khả năng phân bổ")
    void shouldCreateActiveRoomBlockWhenValid() {
      // Given
      UUID actorId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      LocalDate startDate = today.plusDays(1);
      LocalDate endDate = today.plusDays(3);

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.findPhysicalRoomByIdAndHotelId(roomId, hotelId))
          .willReturn(
              Optional.of(
                  new HotelCatalogPort.PhysicalRoomSummary(
                      roomId, hotelId, roomTypeId, "301", "3", "READY", null, null)));
      given(hotelCatalogPort.lockActiveRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(
              List.of(
                  new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(
              roomBlockRepository.findActiveBlocksByRoomIdInRange(
                  roomTypeId, roomId, startDate, endDate))
          .willReturn(List.of());
      given(
              inventoryQueryPort.canAllocateWithCandidateBlock(
                  roomTypeId, null, roomId, startDate, endDate))
          .willReturn(true);
      given(roomBlockRepository.save(any(RoomBlock.class)))
          .willAnswer(
              inv -> {
                RoomBlock b = inv.getArgument(0);
                return new RoomBlock(
                    UUID.randomUUID(),
                    b.hotelId(),
                    b.roomTypeId(),
                    b.roomId(),
                    b.startDate(),
                    b.endDate(),
                    b.reason(),
                    b.status(),
                    b.createdByUserId(),
                    b.createdAt(),
                    b.updatedByUserId(),
                    b.updatedAt(),
                    b.releasedByUserId(),
                    b.releasedAt(),
                    b.releaseReason());
              });

      CreateRoomBlockCommand command =
          new CreateRoomBlockCommand(
              actorId, hotelId, roomId, startDate, endDate, "Bảo trì điều hòa");

      // When
      RoomBlockResult result = createService.execute(command);

      // Then
      assertEquals("ACTIVE", result.status());
      assertEquals(roomId, result.roomId());
      assertEquals("Bảo trì điều hòa", result.reason());
      assertEquals(actorId, result.createdByUserId());
    }

    @Test
    @DisplayName(
        "Từ chối tạo lịch chặn khi làm mất khả năng phân bổ liên tục cho các đơn hiện hữu (BL-06)")
    void shouldRejectWhenCandidateBlockBreaksExistingReservationAllocation() {
      // Given
      UUID actorId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      LocalDate startDate = today.plusDays(1);
      LocalDate endDate = today.plusDays(2);

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.findPhysicalRoomByIdAndHotelId(roomId, hotelId))
          .willReturn(
              Optional.of(
                  new HotelCatalogPort.PhysicalRoomSummary(
                      roomId, hotelId, roomTypeId, "301", "3", "READY", null, null)));
      given(hotelCatalogPort.lockActiveRoomTypesInHotel(hotelId, List.of(roomTypeId)))
          .willReturn(
              List.of(
                  new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(
              roomBlockRepository.findActiveBlocksByRoomIdInRange(
                  roomTypeId, roomId, startDate, endDate))
          .willReturn(List.of());
      given(
              inventoryQueryPort.canAllocateWithCandidateBlock(
                  roomTypeId, null, roomId, startDate, endDate))
          .willReturn(false);

      CreateRoomBlockCommand command =
          new CreateRoomBlockCommand(actorId, hotelId, roomId, startDate, endDate, "Sơn lại tường");

      // When
      InventoryException error =
          assertThrows(InventoryException.class, () -> createService.execute(command));

      // Then
      assertEquals(InventoryErrorCode.ROOM_BLOCK_CONFLICTS_RESERVATION, error.code());
      verify(roomBlockRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("UpdateRoomBlockUseCase & ReleaseRoomBlockUseCase")
  class UpdateAndReleaseTests {

    @Test
    @DisplayName("Không cho phép đổi ngày bắt đầu đã qua của lịch chặn đang diễn ra (BL-04)")
    void shouldRejectChangingPastStartDateOfOngoingBlock() {
      // Given
      UUID actorId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID blockId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();

      RoomBlock existing =
          new RoomBlock(
              blockId,
              hotelId,
              roomTypeId,
              roomId,
              today.minusDays(2),
              today.plusDays(2),
              "Bảo trì ống nước",
              RoomBlock.Status.ACTIVE,
              actorId,
              now.minusSeconds(3600),
              actorId,
              now.minusSeconds(3600),
              null,
              null,
              null);

      given(roomBlockRepository.lockByIdAndHotelId(blockId, hotelId))
          .willReturn(Optional.of(existing));

      UpdateRoomBlockCommand command =
          new UpdateRoomBlockCommand(
              actorId, hotelId, blockId, today.minusDays(1), today.plusDays(3), "Gia hạn bảo trì");

      // When
      InventoryException error =
          assertThrows(InventoryException.class, () -> updateService.execute(command));

      // Then
      assertEquals(InventoryErrorCode.INVALID_DATE_RANGE, error.code());
    }

    @Test
    @DisplayName(
        "Giải phóng lịch chặn chỉ tác động một lần, gọi lặp trả lại bản ghi đã RELEASED (BL-10)")
    void shouldReleaseBlockIdempotently() {
      // Given
      UUID actorId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID blockId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();

      RoomBlock releasedBlock =
          new RoomBlock(
              blockId,
              hotelId,
              roomTypeId,
              roomId,
              today.plusDays(1),
              today.plusDays(2),
              "Bảo trì",
              RoomBlock.Status.RELEASED,
              actorId,
              now.minusSeconds(3600),
              actorId,
              now.minusSeconds(60),
              actorId,
              now.minusSeconds(60),
              "Đã sửa xong sớm");

      given(roomBlockRepository.lockByIdAndHotelId(blockId, hotelId))
          .willReturn(Optional.of(releasedBlock));

      // When
      RoomBlockResult result =
          releaseService.execute(
              new ReleaseRoomBlockCommand(actorId, hotelId, blockId, "Gọi lại lần hai"));

      // Then
      assertEquals("RELEASED", result.status());
      assertEquals("Đã sửa xong sớm", result.releaseReason());
      verify(roomBlockRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("GetRoomCalendarUseCase.execute")
  class GetRoomCalendarTests {

    @Test
    @DisplayName("Tổng hợp đúng số phòng đang phục vụ, bị chặn, đã đặt và còn trống theo từng ngày")
    void shouldBuildRoomCalendarWithDailySummary() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID room301 = UUID.randomUUID();
      UUID room302 = UUID.randomUUID();
      LocalDate d10 = today;
      LocalDate d11 = today.plusDays(1);

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.findActiveRoomTypes(hotelId))
          .willReturn(
              List.of(
                  new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(hotelCatalogPort.findPhysicalRoomsByHotelId(hotelId))
          .willReturn(
              List.of(
                  new HotelCatalogPort.PhysicalRoomSummary(
                      room301, hotelId, roomTypeId, "301", "3", "READY", null, null),
                  new HotelCatalogPort.PhysicalRoomSummary(
                      room302, hotelId, roomTypeId, "302", "3", "DIRTY", null, null)));

      RoomBlock block301OnD10 =
          new RoomBlock(
              UUID.randomUUID(),
              hotelId,
              roomTypeId,
              room301,
              d10,
              d10,
              "Bảo trì",
              RoomBlock.Status.ACTIVE,
              UUID.randomUUID(),
              now,
              UUID.randomUUID(),
              now,
              null,
              null,
              null);

      given(roomBlockRepository.findManagementBlocks(hotelId, null, null, d10, d11))
          .willReturn(List.of(block301OnD10));
      given(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, d10, d10.plusDays(1)))
          .willReturn(1);
      given(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, d11, d11.plusDays(1)))
          .willReturn(1);

      // When
      RoomCalendarResult result =
          calendarService.execute(new GetRoomCalendarQuery(hotelId, null, d10, d11));

      // Then
      assertEquals(1, result.roomTypes().size());
      var rtCal = result.roomTypes().get(0);
      assertEquals(2, rtCal.rooms().size());
      assertEquals(2, rtCal.dailySummaries().size());

      var summaryD10 = rtCal.dailySummaries().get(0);
      assertEquals(2, summaryD10.operationalRooms());
      assertEquals(1, summaryD10.blockedRooms());
      assertEquals(0, summaryD10.reservedRooms());
      assertEquals(1, summaryD10.availableRooms());

      var summaryD11 = rtCal.dailySummaries().get(1);
      assertEquals(2, summaryD11.operationalRooms());
      assertEquals(0, summaryD11.blockedRooms());
      assertEquals(1, summaryD11.reservedRooms());
      assertEquals(1, summaryD11.availableRooms());
    }
  }
}

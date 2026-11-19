package com.team.hotelbooking.inventory.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.inventory.api.exception.InventoryExceptionHandler;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult.DailyInventorySummaryResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult.PhysicalRoomCalendarResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult.RoomTypeCalendarResult;
import com.team.hotelbooking.inventory.application.usecase.CreateRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.GetRoomCalendarUseCase;
import com.team.hotelbooking.inventory.application.usecase.ListRoomBlocksUseCase;
import com.team.hotelbooking.inventory.application.usecase.ReleaseRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomBlockUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller quản lý lịch chặn phòng và lịch phòng")
class ManagementRoomBlockControllerTests {

  private MockMvc mvc;

  @Mock private CreateRoomBlockUseCase createRoomBlockUseCase;
  @Mock private UpdateRoomBlockUseCase updateRoomBlockUseCase;
  @Mock private ReleaseRoomBlockUseCase releaseRoomBlockUseCase;
  @Mock private ListRoomBlocksUseCase listRoomBlocksUseCase;
  @Mock private GetRoomCalendarUseCase getRoomCalendarUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ManagementRoomBlockController(
                    createRoomBlockUseCase,
                    updateRoomBlockUseCase,
                    releaseRoomBlockUseCase,
                    listRoomBlocksUseCase,
                    getRoomCalendarUseCase))
            .setControllerAdvice(new InventoryExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("POST /api/v1/management/hotels/{hotelId}/room-blocks")
  class CreateRoomBlockApiTests {

    @Test
    @DisplayName("Trả về 201 Created và thông tin lịch chặn khi tạo thành công")
    void shouldReturn201WhenRoomBlockCreated() throws Exception {
      // Given
      UUID actorId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      UUID blockId = UUID.randomUUID();
      LocalDate startDate = LocalDate.of(2026, 11, 15);
      LocalDate endDate = LocalDate.of(2026, 11, 16);
      Instant now = Instant.parse("2026-11-10T09:00:00Z");

      RoomBlockResult result =
          new RoomBlockResult(
              blockId,
              hotelId,
              roomTypeId,
              roomId,
              startDate,
              endDate,
              "Bảo trì định kỳ",
              "ACTIVE",
              actorId,
              now,
              actorId,
              now,
              null,
              null,
              null);

      given(createRoomBlockUseCase.execute(any())).willReturn(result);

      Principal principal = actorId::toString;
      String body =
          """
          {
            "roomId": "%s",
            "startDate": "2026-11-15",
            "endDate": "2026-11-16",
            "reason": "Bảo trì định kỳ"
          }
          """
              .formatted(roomId);

      // When & Then
      mvc.perform(
              post("/api/v1/management/hotels/{hotelId}/room-blocks", hotelId)
                  .principal(principal)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.message").value("Tạo lịch chặn phòng thành công."))
          .andExpect(jsonPath("$.data.id").value(blockId.toString()))
          .andExpect(jsonPath("$.data.roomId").value(roomId.toString()))
          .andExpect(jsonPath("$.data.status").value("ACTIVE"))
          .andExpect(jsonPath("$.data.reason").value("Bảo trì định kỳ"));
    }

    @Test
    @DisplayName("Trả về 409 Conflict khi lịch chặn làm mất khả năng phân bổ cho đơn hiện hữu")
    void shouldReturn409WhenBlockConflictsWithExistingReservations() throws Exception {
      // Given
      UUID actorId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();

      given(createRoomBlockUseCase.execute(any()))
          .willThrow(
              InventoryException.roomBlockConflictsReservation(
                  "Không thể chặn phòng vì ảnh hưởng đơn đã xác nhận."));

      Principal principal = actorId::toString;
      String body =
          """
          {
            "roomId": "%s",
            "startDate": "2026-11-15",
            "endDate": "2026-11-16",
            "reason": "Bảo trì"
          }
          """
              .formatted(roomId);

      // When & Then
      mvc.perform(
              post("/api/v1/management/hotels/{hotelId}/room-blocks", hotelId)
                  .principal(principal)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.detail").value("Không thể chặn phòng vì ảnh hưởng đơn đã xác nhận."));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/room-calendar")
  class GetRoomCalendarApiTests {

    @Test
    @DisplayName("Trả về 200 OK, header Cache-Control no-store và lịch phòng chi tiết")
    void shouldReturn200AndRoomCalendar() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      LocalDate startDate = LocalDate.of(2026, 11, 10);
      LocalDate endDate = LocalDate.of(2026, 11, 11);

      RoomCalendarResult result =
          new RoomCalendarResult(
              hotelId,
              startDate,
              endDate,
              List.of(
                  new RoomTypeCalendarResult(
                      roomTypeId,
                      "Deluxe",
                      List.of(
                          new PhysicalRoomCalendarResult(
                              roomId, "301", "3", "READY", null, null, List.of())),
                      List.of(new DailyInventorySummaryResult(startDate, 1, 0, 0, 1)))));

      given(getRoomCalendarUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              get("/api/v1/management/hotels/{hotelId}/room-calendar", hotelId)
                  .param("startDate", "2026-11-10")
                  .param("endDate", "2026-11-11"))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Lấy lịch phòng thành công."))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.roomTypes[0].roomTypeName").value("Deluxe"))
          .andExpect(jsonPath("$.data.roomTypes[0].rooms[0].roomNumber").value("301"))
          .andExpect(jsonPath("$.data.roomTypes[0].dailySummaries[0].availableRooms").value(1));
    }
  }
}

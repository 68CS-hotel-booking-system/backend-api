package com.team.hotelbooking.inventory.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.inventory.api.dto.request.AssignRoomsRequest;
import com.team.hotelbooking.inventory.api.dto.request.AssignRoomsRequest.RoomAssignmentItemRequest;
import com.team.hotelbooking.inventory.api.dto.request.MarkNoShowRequest;
import com.team.hotelbooking.inventory.api.dto.request.RecordLateArrivalRequest;
import com.team.hotelbooking.inventory.api.dto.request.UpdateRoomReadinessRequest;
import com.team.hotelbooking.inventory.api.exception.InventoryExceptionHandler;
import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;
import com.team.hotelbooking.inventory.application.usecase.AssignRoomsUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckInBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckOutBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.MarkNoShowUseCase;
import com.team.hotelbooking.inventory.application.usecase.RecordLateArrivalUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomReadinessUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller phân phòng vật lý và vận hành lưu trú")
class ManagementStayOperationControllerTests {

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @Mock private AssignRoomsUseCase assignRoomsUseCase;
  @Mock private CheckInBookingUseCase checkInBookingUseCase;
  @Mock private CheckOutBookingUseCase checkOutBookingUseCase;
  @Mock private RecordLateArrivalUseCase recordLateArrivalUseCase;
  @Mock private MarkNoShowUseCase markNoShowUseCase;
  @Mock private UpdateRoomReadinessUseCase updateRoomReadinessUseCase;

  private final UUID actorId = UUID.randomUUID();
  private final UUID hotelId = UUID.randomUUID();
  private final UUID bookingId = UUID.randomUUID();
  private final Principal principal = actorId::toString;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ManagementStayOperationController(
                    assignRoomsUseCase,
                    checkInBookingUseCase,
                    checkOutBookingUseCase,
                    recordLateArrivalUseCase,
                    markNoShowUseCase,
                    updateRoomReadinessUseCase))
            .setControllerAdvice(new InventoryExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Test
  @DisplayName("PUT .../room-assignments trả về 200 khi phân công phòng thành công")
  void shouldAssignRoomsSuccessfully() throws Exception {
    UUID itemId = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();
    AssignRoomsRequest request =
        new AssignRoomsRequest(
            List.of(new RoomAssignmentItemRequest(itemId, List.of(roomId))), "Phân phòng lần đầu");

    RoomAssignmentResult assignmentResult =
        new RoomAssignmentResult(
            UUID.randomUUID(),
            hotelId,
            bookingId,
            itemId,
            UUID.randomUUID(),
            roomId,
            "301",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12),
            "ASSIGNED",
            actorId,
            Instant.now(),
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    given(assignRoomsUseCase.execute(any()))
        .willReturn(
            new BookingRoomAssignmentsResult(
                hotelId, bookingId, List.of(assignmentResult), List.of(assignmentResult)));

    mvc.perform(
            put(
                    "/api/v1/management/hotels/{hotelId}/bookings/{bookingId}/room-assignments",
                    hotelId,
                    bookingId)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Phân công phòng vật lý thành công."))
        .andExpect(jsonPath("$.data.activeAssignments[0].roomNumberSnapshot").value("301"));
  }

  @Test
  @DisplayName("POST .../check-in trả về 409 khi phòng vật lý chưa READY")
  void shouldReturn409WhenCheckInRoomNotReady() throws Exception {
    given(checkInBookingUseCase.execute(any()))
        .willThrow(
            InventoryException.roomNotReady(
                "Phòng vật lý 301 chưa ở trạng thái sẵn sàng (READY)."));

    mvc.perform(
            post(
                    "/api/v1/management/hotels/{hotelId}/bookings/{bookingId}/check-in",
                    hotelId,
                    bookingId)
                .principal(principal))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_READY"));
  }

  @Test
  @DisplayName("POST .../check-out trả về 200 khi trả phòng thành công")
  void shouldCheckOutSuccessfully() throws Exception {
    given(checkOutBookingUseCase.execute(any()))
        .willReturn(
            new BookingStayOperationResult(
                hotelId,
                bookingId,
                "CHECKED_OUT",
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                null,
                null,
                0,
                Instant.now(),
                List.of()));

    mvc.perform(
            post(
                    "/api/v1/management/hotels/{hotelId}/bookings/{bookingId}/check-out",
                    hotelId,
                    bookingId)
                .principal(principal))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.stayStatus").value("CHECKED_OUT"));
  }

  @Test
  @DisplayName("POST .../late-arrival trả về 200 khi ghi nhận lịch đến muộn thành công")
  void shouldRecordLateArrivalSuccessfully() throws Exception {
    Instant expectedArrival = Instant.parse("2026-11-11T03:00:00Z");
    RecordLateArrivalRequest request =
        new RecordLateArrivalRequest(expectedArrival, "Khách báo đến muộn");

    given(recordLateArrivalUseCase.execute(any()))
        .willReturn(
            new BookingStayOperationResult(
                hotelId,
                bookingId,
                "CONFIRMED",
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                expectedArrival,
                "Khách báo đến muộn",
                0,
                Instant.now(),
                List.of()));

    mvc.perform(
            post(
                    "/api/v1/management/hotels/{hotelId}/bookings/{bookingId}/late-arrival",
                    hotelId,
                    bookingId)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lateArrivalNote").value("Khách báo đến muộn"));
  }

  @Test
  @DisplayName("POST .../no-show trả về 200 và số đêm hoàn trả tồn kho")
  void shouldMarkNoShowSuccessfully() throws Exception {
    MarkNoShowRequest request = new MarkNoShowRequest("Khách không đến nhận phòng");

    given(markNoShowUseCase.execute(any()))
        .willReturn(
            new BookingStayOperationResult(
                hotelId,
                bookingId,
                "NO_SHOW",
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                null,
                null,
                1,
                Instant.now(),
                List.of()));

    mvc.perform(
            post(
                    "/api/v1/management/hotels/{hotelId}/bookings/{bookingId}/no-show",
                    hotelId,
                    bookingId)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.stayStatus").value("NO_SHOW"))
        .andExpect(jsonPath("$.data.releasedRoomNights").value(1));
  }

  @Test
  @DisplayName("PATCH .../rooms/{roomId}/readiness trả về 200 khi cập nhật trạng thái READY")
  void shouldUpdateRoomReadinessSuccessfully() throws Exception {
    UUID roomId = UUID.randomUUID();
    UpdateRoomReadinessRequest request = new UpdateRoomReadinessRequest("READY");

    given(updateRoomReadinessUseCase.execute(any()))
        .willReturn(
            new RoomReadinessResult(
                hotelId, roomId, UUID.randomUUID(), "301", "3", "READY", Instant.now()));

    mvc.perform(
            patch("/api/v1/management/hotels/{hotelId}/rooms/{roomId}/readiness", hotelId, roomId)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.readiness").value("READY"));
  }
}

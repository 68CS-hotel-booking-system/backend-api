package com.team.hotelbooking.hotel.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.hotel.api.dto.request.UpdateRoomRequest;
import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.result.RoomResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.Instant;
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
@DisplayName("Controller quản lý phòng vật lý (HT21)")
class ManagementRoomControllerTests {

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @Mock private CreateRoomUseCase createRoomUseCase;
  @Mock private UpdateRoomUseCase updateRoomUseCase;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ManagementRoomController(createRoomUseCase, updateRoomUseCase))
            .setControllerAdvice(new HotelExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}/rooms/{roomId}")
  class UpdateRoomTests {

    @Test
    @DisplayName("Nên trả về 200 khi cập nhật phòng vật lý thành công")
    void shouldReturn200WhenUpdateSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();

      UpdateRoomRequest request = new UpdateRoomRequest("P.102", (short) 1, "cleaning", true);

      RoomResult result =
          new RoomResult(
              roomId,
              hotelId,
              roomTypeId,
              "P.102",
              (short) 1,
              "cleaning",
              true,
              Instant.now(),
              Instant.now());

      given(updateRoomUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              patch("/api/v1/management/hotels/{hotelId}/rooms/{roomId}", hotelId, roomId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Cập nhật phòng thành công."))
          .andExpect(jsonPath("$.data.id").value(roomId.toString()))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.roomNumber").value("P.102"))
          .andExpect(jsonPath("$.data.status").value("cleaning"))
          .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("Nên trả về 404 khi không tìm thấy phòng hoặc khách sạn")
    void shouldReturn404WhenRoomNotFound() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();

      UpdateRoomRequest request = new UpdateRoomRequest("P.102", null, null, null);

      given(updateRoomUseCase.execute(any()))
          .willThrow(
              HotelException.roomNotFound(
                  "Không tìm thấy phòng vật lý hoặc phòng không thuộc khách sạn."));

      // When & Then
      mvc.perform(
              patch("/api/v1/management/hotels/{hotelId}/rooms/{roomId}", hotelId, roomId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.detail")
                  .value("Không tìm thấy phòng vật lý hoặc phòng không thuộc khách sạn."));
    }

    @Test
    @DisplayName("Nên trả về 409 khi số phòng bị trùng lặp")
    void shouldReturn409WhenDuplicateRoomNumber() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();

      UpdateRoomRequest request = new UpdateRoomRequest("P.201", null, null, null);

      given(updateRoomUseCase.execute(any()))
          .willThrow(
              HotelException.duplicateRoomNumber(
                  "Số phòng 'P.201' đã tồn tại trong khách sạn này."));

      // When & Then
      mvc.perform(
              patch("/api/v1/management/hotels/{hotelId}/rooms/{roomId}", hotelId, roomId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.detail").value("Số phòng 'P.201' đã tồn tại trong khách sạn này."));
    }
  }
}

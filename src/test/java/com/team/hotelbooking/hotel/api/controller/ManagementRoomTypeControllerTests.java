package com.team.hotelbooking.hotel.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.hotel.api.dto.request.UpdateRoomTypeRequest;
import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomTypesUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
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
@DisplayName("Controller quản lý loại phòng (HT17)")
class ManagementRoomTypeControllerTests {

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @Mock private CreateRoomTypeUseCase createRoomTypeUseCase;
  @Mock private UpdateRoomTypeUseCase updateRoomTypeUseCase;
  @Mock private ListManagementRoomTypesUseCase listManagementRoomTypesUseCase;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ManagementRoomTypeController(
                    createRoomTypeUseCase, updateRoomTypeUseCase, listManagementRoomTypesUseCase))
            .setControllerAdvice(new HotelExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}")
  class UpdateRoomTypeTests {

    @Test
    @DisplayName("Nên trả về 200 khi cập nhật loại phòng thành công")
    void shouldReturn200WhenUpdateSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();

      UpdateRoomTypeRequest request =
          new UpdateRoomTypeRequest(
              "Phòng VIP", "Mô tả mới", (short) 2, (short) 1, (short) 3, 2_000_000L, true);

      RoomTypeResult result =
          new RoomTypeResult(
              roomTypeId,
              hotelId,
              "Phòng VIP",
              "Mô tả mới",
              (short) 2,
              (short) 1,
              (short) 3,
              2_000_000L,
              true,
              Instant.now(),
              Instant.now());

      given(updateRoomTypeUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              patch(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Cập nhật loại phòng thành công."))
          .andExpect(jsonPath("$.data.id").value(roomTypeId.toString()))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.name").value("Phòng VIP"))
          .andExpect(jsonPath("$.data.basePrice").value(2_000_000))
          .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("Nên trả về 400 khi request body có maxAdults < 1")
    void shouldReturn400WhenMaxAdultsIsInvalid() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();

      UpdateRoomTypeRequest invalidRequest =
          new UpdateRoomTypeRequest(null, null, (short) 0, null, null, null, null);

      // When & Then
      mvc.perform(
              patch(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Nên trả về 404 khi loại phòng hoặc khách sạn không tồn tại")
    void shouldReturn404WhenRoomTypeNotFound() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();

      UpdateRoomTypeRequest request =
          new UpdateRoomTypeRequest("Phòng VIP", null, null, null, null, null, null);

      given(updateRoomTypeUseCase.execute(any()))
          .willThrow(
              HotelException.roomTypeNotFound(
                  "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

      // When & Then
      mvc.perform(
              patch(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.detail")
                  .value("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));
    }
  }
}

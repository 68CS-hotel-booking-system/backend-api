package com.team.hotelbooking.inventory.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.inventory.api.exception.InventoryExceptionHandler;
import com.team.hotelbooking.inventory.application.result.CheckAvailabilityResult;
import com.team.hotelbooking.inventory.application.result.RoomTypeAvailabilityResult;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller tra cứu phòng trống")
class AvailabilityControllerTests {

  private MockMvc mvc;

  @Mock private CheckAvailabilityUseCase checkAvailabilityUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new AvailabilityController(checkAvailabilityUseCase))
            .setControllerAdvice(new InventoryExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("GET /api/v1/hotels/{hotelId}/availability")
  class GetHotelAvailabilityTests {

    @Test
    @DisplayName("Nên trả về 200 và danh sách phòng trống khi tra cứu thành công")
    void shouldReturnAvailabilitySuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.now().plusDays(2);
      LocalDate checkOut = LocalDate.now().plusDays(5);

      RoomTypeAvailabilityResult rt =
          new RoomTypeAvailabilityResult(
              roomTypeId, "Deluxe King", 3, 2, 1, 4, true, 1200000L, 3600000L, "VND");

      CheckAvailabilityResult result =
          new CheckAvailabilityResult(hotelId, checkIn, checkOut, 3, List.of(rt));

      given(checkAvailabilityUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              get("/api/v1/hotels/{hotelId}/availability", hotelId)
                  .param("checkInDate", checkIn.toString())
                  .param("checkOutDate", checkOut.toString())
                  .param("adults", "2")
                  .param("children", "1")
                  .param("quantity", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Thành công."))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.totalNights").value(3))
          .andExpect(jsonPath("$.data.roomTypes[0].roomTypeId").value(roomTypeId.toString()))
          .andExpect(jsonPath("$.data.roomTypes[0].availableQuantity").value(4))
          .andExpect(jsonPath("$.data.roomTypes[0].isAvailable").value(true));
    }

    @Test
    @DisplayName("Nên trả về 404 khi không tìm thấy khách sạn")
    void shouldReturn404WhenHotelNotFound() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.now().plusDays(1);
      LocalDate checkOut = LocalDate.now().plusDays(3);

      given(checkAvailabilityUseCase.execute(any()))
          .willThrow(InventoryException.hotelNotFound("Không tìm thấy khách sạn."));

      // When & Then
      mvc.perform(
              get("/api/v1/hotels/{hotelId}/availability", hotelId)
                  .param("checkInDate", checkIn.toString())
                  .param("checkOutDate", checkOut.toString()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.title").value("Not Found"))
          .andExpect(jsonPath("$.detail").value("Không tìm thấy khách sạn."));
    }

    @Test
    @DisplayName("Nên trả về 400 khi khoảng thời gian không hợp lệ")
    void shouldReturn400WhenInvalidDateRange() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.now().plusDays(5);
      LocalDate checkOut = LocalDate.now().plusDays(2);

      given(checkAvailabilityUseCase.execute(any()))
          .willThrow(
              InventoryException.invalidDateRange(
                  "Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm."));

      // When & Then
      mvc.perform(
              get("/api/v1/hotels/{hotelId}/availability", hotelId)
                  .param("checkInDate", checkIn.toString())
                  .param("checkOutDate", checkOut.toString()))
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.detail")
                  .value("Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm."));
    }
  }
}

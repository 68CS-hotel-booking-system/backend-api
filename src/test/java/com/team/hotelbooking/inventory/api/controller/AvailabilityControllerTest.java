package com.team.hotelbooking.inventory.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.inventory.api.exception.InventoryExceptionHandler;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.HotelAvailability;
import com.team.hotelbooking.inventory.domain.model.RoomTypeAvailability;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

  private MockMvc mvc;

  @Mock private CheckAvailabilityUseCase checkAvailabilityUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new AvailabilityController(checkAvailabilityUseCase))
            .setControllerAdvice(new InventoryExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Test
  void shouldReturnAvailabilitySuccessfully() throws Exception {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate checkIn = LocalDate.now().plusDays(2);
    LocalDate checkOut = LocalDate.now().plusDays(5);

    RoomTypeAvailability rt =
        new RoomTypeAvailability(
            roomTypeId, "Deluxe King", 3, 2, 1, 4, true, 1200000L, 3600000L, "VND");

    HotelAvailability availability =
        new HotelAvailability(hotelId, checkIn, checkOut, 3, List.of(rt));

    when(checkAvailabilityUseCase.execute(any())).thenReturn(availability);

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
  void shouldReturn404WhenHotelNotFound() throws Exception {
    UUID hotelId = UUID.randomUUID();
    LocalDate checkIn = LocalDate.now().plusDays(1);
    LocalDate checkOut = LocalDate.now().plusDays(3);

    when(checkAvailabilityUseCase.execute(any()))
        .thenThrow(InventoryException.hotelNotFound("Không tìm thấy khách sạn."));

    mvc.perform(
            get("/api/v1/hotels/{hotelId}/availability", hotelId)
                .param("checkInDate", checkIn.toString())
                .param("checkOutDate", checkOut.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Không tìm thấy khách sạn."));
  }

  @Test
  void shouldReturn400WhenInvalidDateRange() throws Exception {
    UUID hotelId = UUID.randomUUID();
    LocalDate checkIn = LocalDate.now().plusDays(5);
    LocalDate checkOut = LocalDate.now().plusDays(2);

    when(checkAvailabilityUseCase.execute(any()))
        .thenThrow(
            InventoryException.invalidDateRange(
                "Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm."));

    mvc.perform(
            get("/api/v1/hotels/{hotelId}/availability", hotelId)
                .param("checkInDate", checkIn.toString())
                .param("checkOutDate", checkOut.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value("Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm."));
  }
}

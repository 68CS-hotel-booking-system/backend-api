package com.team.hotelbooking.pricing.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.pricing.api.exception.PricingExceptionHandler;
import com.team.hotelbooking.pricing.application.result.DailyRateResult;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller tra cứu giá phòng theo ngày")
class ManagementRateControllerTests {

  private MockMvc mvc;

  @Mock private GetRoomRatesUseCase getRoomRatesUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ManagementRateController(getRoomRatesUseCase))
            .setControllerAdvice(new PricingExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates")
  class GetRoomRatesTests {

    @Test
    @DisplayName("Nên trả về 200 và danh sách giá theo ngày khi tra cứu thành công")
    void shouldReturnRatesSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate startDate = LocalDate.of(2026, 11, 1);
      LocalDate endDate = LocalDate.of(2026, 11, 3);

      List<DailyRateResult> rates =
          List.of(
              new DailyRateResult(LocalDate.of(2026, 11, 1), 1200000L, true, Instant.now()),
              new DailyRateResult(LocalDate.of(2026, 11, 2), null, false, null),
              new DailyRateResult(LocalDate.of(2026, 11, 3), 1500000L, true, Instant.now()));

      RoomTypeRatesResult result =
          new RoomTypeRatesResult(hotelId, roomTypeId, "VND", startDate, endDate, rates);

      given(getRoomRatesUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              get(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .param("startDate", "2026-11-01")
                  .param("endDate", "2026-11-03"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Thành công."))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.roomTypeId").value(roomTypeId.toString()))
          .andExpect(jsonPath("$.data.rates[0].price").value(1200000))
          .andExpect(jsonPath("$.data.rates[0].hasRate").value(true))
          .andExpect(jsonPath("$.data.rates[1].hasRate").value(false))
          .andExpect(jsonPath("$.data.rates[2].price").value(1500000));
    }

    @Test
    @DisplayName("Nên trả về 404 khi loại phòng không thuộc khách sạn")
    void shouldReturn404WhenRoomTypeNotFound() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();

      given(getRoomRatesUseCase.execute(any()))
          .willThrow(
              PricingException.roomTypeNotFound(
                  "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

      // When & Then
      mvc.perform(
              get(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .param("startDate", "2026-11-01")
                  .param("endDate", "2026-11-03"))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.detail")
                  .value("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));
    }
  }
}

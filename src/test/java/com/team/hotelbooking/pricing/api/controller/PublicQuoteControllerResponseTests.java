package com.team.hotelbooking.pricing.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.pricing.api.dto.request.CalculateStayQuoteRequest;
import com.team.hotelbooking.pricing.api.dto.request.CalculateStayQuoteRequest.QuoteItemRequest;
import com.team.hotelbooking.pricing.api.exception.PricingExceptionHandler;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult.ItemQuoteResult;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult.NightlyPriceResult;
import com.team.hotelbooking.pricing.application.usecase.CalculateStayQuoteUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response của PublicQuoteController")
class PublicQuoteControllerResponseTests {

  private static final UUID HOTEL_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final UUID DELUXE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
  private static final String PATH = "/api/v1/hotels/" + HOTEL_ID + "/quote";

  @Mock private CalculateStayQuoteUseCase calculateStayQuoteUseCase;

  private MockMvc mvc;
  private JsonMapper jsonMapper;

  @BeforeEach
  void setUp() {
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();
    new JacksonConfig().jsonCustomizer().customize(mapperBuilder);
    jsonMapper = mapperBuilder.build();
    JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(mapperBuilder);
    mvc =
        MockMvcBuilders.standaloneSetup(new PublicQuoteController(calculateStayQuoteUseCase))
            .setMessageConverters(converter)
            .setControllerAdvice(
                new PricingExceptionHandler(new ProblemDetailFactory()),
                new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("POST /api/v1/hotels/{hotelId}/quote")
  class CalculateQuoteApiTests {

    @Test
    @DisplayName("Trả về 200 kèm chi tiết ngày/đêm, giờ nhận/trả, tiền phòng và phụ thu khi hợp lệ")
    void shouldReturn200AndQuoteBreakdownWhenRequestIsValid() throws Exception {
      // Given
      CalculateStayQuoteRequest request =
          new CalculateStayQuoteRequest(
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              List.of(new QuoteItemRequest(DELUXE_ID, 1, 2, 1)));

      StayQuoteResult result =
          new StayQuoteResult(
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              3,
              2,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              "VND",
              2_000_000L,
              500_000L,
              0L,
              200_000L,
              700_000L,
              2_700_000L,
              0L,
              2_700_000L,
              List.of(
                  new ItemQuoteResult(
                      DELUXE_ID,
                      "Deluxe King",
                      1,
                      2,
                      1,
                      1,
                      2_000_000L,
                      500_000L,
                      0L,
                      200_000L,
                      700_000L,
                      2_700_000L,
                      List.of(
                          new NightlyPriceResult(LocalDate.of(2026, 11, 10), 1_000_000L),
                          new NightlyPriceResult(LocalDate.of(2026, 11, 11), 1_000_000L)))));

      given(calculateStayQuoteUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              post(PATH)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.message").value("Báo giá lưu trú thành công."))
          .andExpect(jsonPath("$.data.hotelId").value(HOTEL_ID.toString()))
          .andExpect(jsonPath("$.data.checkIn").value("2026-11-10"))
          .andExpect(jsonPath("$.data.checkOut").value("2026-11-12"))
          .andExpect(jsonPath("$.data.days").value(3))
          .andExpect(jsonPath("$.data.nights").value(2))
          .andExpect(jsonPath("$.data.standardCheckInTime").value("14:00:00"))
          .andExpect(jsonPath("$.data.standardCheckOutTime").value("12:00:00"))
          .andExpect(jsonPath("$.data.expectedCheckInTime").value("08:00:00"))
          .andExpect(jsonPath("$.data.expectedCheckOutTime").value("12:00:00"))
          .andExpect(jsonPath("$.data.roomSubtotalAmount").value(2000000))
          .andExpect(jsonPath("$.data.earlyCheckInSurchargeAmount").value(500000))
          .andExpect(jsonPath("$.data.lateCheckOutSurchargeAmount").value(0))
          .andExpect(jsonPath("$.data.extraGuestSurchargeAmount").value(200000))
          .andExpect(jsonPath("$.data.surchargeAmount").value(700000))
          .andExpect(jsonPath("$.data.subtotalAmount").value(2700000))
          .andExpect(jsonPath("$.data.discountAmount").value(0))
          .andExpect(jsonPath("$.data.totalAmount").value(2700000))
          .andExpect(jsonPath("$.data.items", hasSize(1)))
          .andExpect(jsonPath("$.data.items[0].roomTypeId").value(DELUXE_ID.toString()))
          .andExpect(jsonPath("$.data.items[0].nightlyPrices", hasSize(2)));
    }

    @Test
    @DisplayName("Trả về 400 ProblemDetail khi vượt quá sức chứa tối đa của loại phòng")
    void shouldReturn400WhenOccupancyInvalid() throws Exception {
      // Given
      CalculateStayQuoteRequest request =
          new CalculateStayQuoteRequest(
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              null,
              null,
              List.of(new QuoteItemRequest(DELUXE_ID, 1, 5, 0)));

      given(calculateStayQuoteUseCase.execute(any()))
          .willThrow(
              PricingException.invalidOccupancy(
                  "Số lượng khách vượt quá sức chứa tối đa của loại phòng."));

      // When & Then
      mvc.perform(
              post(PATH)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.code").value("INVALID_OCCUPANCY"));
    }
  }
}

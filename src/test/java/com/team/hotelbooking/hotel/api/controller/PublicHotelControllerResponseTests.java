package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import com.team.hotelbooking.hotel.application.usecase.GetPublicHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response của PublicHotelController")
class PublicHotelControllerResponseTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID CITY_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String PATH = "/api/v1/hotels/" + HOTEL_ID;

  @Mock private GetPublicHotelUseCase getPublicHotelUseCase;

  @InjectMocks private PublicHotelController controller;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();
    new JacksonConfig().jsonCustomizer().customize(mapperBuilder);
    JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(mapperBuilder);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .setControllerAdvice(
                new HotelExceptionHandler(new ProblemDetailFactory()),
                new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("GET /api/v1/hotels/{hotelId}")
  class GetPublicHotelApiTests {

    @Test
    void shouldReturnOnlyPublicFieldsWhenHotelIsActive() throws Exception {
      // Given
      given(getPublicHotelUseCase.execute(HOTEL_ID))
          .willReturn(
              new GetPublicHotelResult(
                  HOTEL_ID,
                  CITY_ID,
                  "Khách sạn Biển Xanh",
                  "khach-san-bien-xanh",
                  null,
                  "12 Võ Nguyên Giáp",
                  new BigDecimal("16.0614"),
                  null,
                  (short) 4,
                  LocalTime.of(14, 0),
                  LocalTime.of(12, 30)));

      // When
      ResultActions response = mvc.perform(get(PATH));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(jsonPath("$.*", hasSize(2)))
          .andExpect(jsonPath("$.message").value("Lấy thông tin khách sạn thành công."))
          .andExpect(jsonPath("$.data.*", hasSize(11)))
          .andExpect(jsonPath("$.data.id").value(HOTEL_ID.toString()))
          .andExpect(jsonPath("$.data.cityId").value(CITY_ID.toString()))
          .andExpect(jsonPath("$.data.name").value("Khách sạn Biển Xanh"))
          .andExpect(jsonPath("$.data.slug").value("khach-san-bien-xanh"))
          .andExpect(jsonPath("$.data.description").isEmpty())
          .andExpect(jsonPath("$.data.address").value("12 Võ Nguyên Giáp"))
          .andExpect(jsonPath("$.data.latitude").value(16.0614))
          .andExpect(jsonPath("$.data.longitude").isEmpty())
          .andExpect(jsonPath("$.data.starRating").value(4))
          .andExpect(jsonPath("$.data.checkInTime").value("14:00:00"))
          .andExpect(jsonPath("$.data.checkOutTime").value("12:30:00"))
          .andExpect(jsonPath("$.data.ownerId").doesNotExist())
          .andExpect(jsonPath("$.data.status").doesNotExist())
          .andExpect(jsonPath("$.data.version").doesNotExist())
          .andExpect(jsonPath("$.data.createdAt").doesNotExist())
          .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void shouldReturnProblemDetailWhenHotelIsNotPublic() throws Exception {
      // Given
      given(getPublicHotelUseCase.execute(HOTEL_ID))
          .willThrow(new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

      // When
      ResultActions response = mvc.perform(get(PATH));

      // Then
      response
          .andExpect(status().isNotFound())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"))
          .andExpect(jsonPath("$.detail").value("Không tìm thấy khách sạn."))
          .andExpect(jsonPath("$.instance").value(PATH));
    }

    @Test
    void shouldReturnBadRequestWhenHotelIdIsNotUuid() throws Exception {
      // When
      ResultActions response = mvc.perform(get("/api/v1/hotels/not-a-uuid"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
  }
}

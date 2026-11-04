package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ListAmenitiesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.util.List;
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
@DisplayName("Hợp đồng response của AmenityController")
class AmenityControllerResponseTests {

  private static final String MESSAGE = "Lấy danh sách tiện ích thành công.";
  private static final UUID SWIMMING_POOL_ID =
      UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3");
  private static final UUID FREE_WIFI_ID = UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a");

  @Mock private ListAmenitiesUseCase listAmenitiesUseCase;

  @InjectMocks private AmenityController controller;

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
  @DisplayName("GET /api/v1/amenities")
  class ListAmenitiesApiTests {

    @Test
    void shouldReturnAllAmenitiesWhenCategoryIsMissing() throws Exception {
      // Given
      given(listAmenitiesUseCase.execute(null))
          .willReturn(
              List.of(
                  new ListAmenitiesResult(SWIMMING_POOL_ID, "SWIMMING_POOL", "Hồ bơi", "HOTEL"),
                  new ListAmenitiesResult(FREE_WIFI_ID, "FREE_WIFI", "Wi-Fi miễn phí", "ROOM")));

      // When
      ResultActions response = mvc.perform(get("/api/v1/amenities"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(jsonPath("$.*", hasSize(2)))
          .andExpect(jsonPath("$.message").value(MESSAGE))
          .andExpect(jsonPath("$.data", hasSize(2)))
          .andExpect(jsonPath("$.data[0].*", hasSize(4)))
          .andExpect(jsonPath("$.data[0].id").value(SWIMMING_POOL_ID.toString()))
          .andExpect(jsonPath("$.data[0].code").value("SWIMMING_POOL"))
          .andExpect(jsonPath("$.data[0].name").value("Hồ bơi"))
          .andExpect(jsonPath("$.data[0].category").value("HOTEL"))
          .andExpect(jsonPath("$.data[1].*", hasSize(4)))
          .andExpect(jsonPath("$.data[1].id").value(FREE_WIFI_ID.toString()))
          .andExpect(jsonPath("$.data[1].code").value("FREE_WIFI"))
          .andExpect(jsonPath("$.data[1].name").value("Wi-Fi miễn phí"))
          .andExpect(jsonPath("$.data[1].category").value("ROOM"));
    }

    @Test
    void shouldPassCategoryToUseCaseWhenCategoryIsProvided() throws Exception {
      // Given
      given(listAmenitiesUseCase.execute("HOTEL"))
          .willReturn(
              List.of(
                  new ListAmenitiesResult(SWIMMING_POOL_ID, "SWIMMING_POOL", "Hồ bơi", "HOTEL")));

      // When
      ResultActions response = mvc.perform(get("/api/v1/amenities").param("category", "HOTEL"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data", hasSize(1)))
          .andExpect(jsonPath("$.data[0].category").value("HOTEL"));
    }

    @Test
    void shouldReturnEmptyArrayWhenNoAmenityExists() throws Exception {
      // Given
      given(listAmenitiesUseCase.execute(null)).willReturn(List.of());

      // When
      ResultActions response = mvc.perform(get("/api/v1/amenities"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(jsonPath("$.message").value(MESSAGE))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void shouldReturnProblemDetailWhenCategoryIsInvalid() throws Exception {
      // Given
      given(listAmenitiesUseCase.execute("hotel"))
          .willThrow(HotelException.invalid("Loại tiện ích phải là HOTEL hoặc ROOM."));

      // When
      ResultActions response = mvc.perform(get("/api/v1/amenities").param("category", "hotel"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.detail").value("Loại tiện ích phải là HOTEL hoặc ROOM."))
          .andExpect(jsonPath("$.instance").value("/api/v1/amenities"));
    }
  }
}

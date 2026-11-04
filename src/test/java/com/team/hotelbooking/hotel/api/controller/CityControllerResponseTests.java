package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.result.ListCitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ListCitiesUseCase;
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
@DisplayName("Hợp đồng response của CityController")
class CityControllerResponseTests {

  private static final String MESSAGE = "Lấy danh sách thành phố thành công.";
  private static final UUID HA_NOI_ID = UUID.fromString("81560b64-8216-46c6-8f1b-f14214fc0c90");
  private static final UUID HUE_ID = UUID.fromString("d4266df8-21c1-4f42-849a-30bebfc561b9");

  @Mock private ListCitiesUseCase listCitiesUseCase;

  @InjectMocks private CityController controller;

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
  @DisplayName("GET /api/v1/cities")
  class ListCitiesApiTests {

    @Test
    void shouldReturnCitiesInUseCaseOrderWhenCitiesExist() throws Exception {
      // Given
      given(listCitiesUseCase.execute())
          .willReturn(
              List.of(
                  new ListCitiesResult(HA_NOI_ID, "VN", "Hà Nội", "ha-noi"),
                  new ListCitiesResult(HUE_ID, "VN", "Huế", "hue")));

      // When
      ResultActions response = mvc.perform(get("/api/v1/cities"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(jsonPath("$.*", hasSize(2)))
          .andExpect(jsonPath("$.message").value(MESSAGE))
          .andExpect(jsonPath("$.data", hasSize(2)))
          .andExpect(jsonPath("$.data[0].*", hasSize(4)))
          .andExpect(jsonPath("$.data[0].id").value(HA_NOI_ID.toString()))
          .andExpect(jsonPath("$.data[0].countryCode").value("VN"))
          .andExpect(jsonPath("$.data[0].name").value("Hà Nội"))
          .andExpect(jsonPath("$.data[0].slug").value("ha-noi"))
          .andExpect(jsonPath("$.data[1].*", hasSize(4)))
          .andExpect(jsonPath("$.data[1].id").value(HUE_ID.toString()))
          .andExpect(jsonPath("$.data[1].countryCode").value("VN"))
          .andExpect(jsonPath("$.data[1].name").value("Huế"))
          .andExpect(jsonPath("$.data[1].slug").value("hue"));
    }

    @Test
    void shouldReturnEmptyArrayWhenNoCityExists() throws Exception {
      // Given
      given(listCitiesUseCase.execute()).willReturn(List.of());

      // When
      ResultActions response = mvc.perform(get("/api/v1/cities"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(jsonPath("$.message").value(MESSAGE))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data", hasSize(0)));
    }
  }
}

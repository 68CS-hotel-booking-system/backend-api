package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.command.ListManagementRoomTypesQuery;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomTypesUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.security.Principal;
import java.time.Instant;
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
@DisplayName("Hợp đồng response danh sách loại phòng quản lý")
class ManagementRoomTypeControllerResponseTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b");
  private static final String PATH = "/api/v1/management/hotels/" + HOTEL_ID + "/room-types";
  private static final Instant CREATED_AT = Instant.parse("2026-11-04T01:02:03Z");
  private static final Instant UPDATED_AT = Instant.parse("2026-11-04T04:05:06Z");
  private static final Principal ACTOR = () -> ACTOR_ID.toString();

  @Mock private CreateRoomTypeUseCase createRoomTypeUseCase;

  @Mock private ListManagementRoomTypesUseCase listManagementRoomTypesUseCase;

  @Mock private UpdateRoomTypeUseCase updateRoomTypeUseCase;

  @InjectMocks private ManagementRoomTypeController controller;

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
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/room-types")
  class ListManagementRoomTypesApiTests {

    @Test
    void shouldReturnRoomTypesWithStatusAndVersionWhenActorManagesHotel() throws Exception {
      // Given
      given(
              listManagementRoomTypesUseCase.execute(
                  new ListManagementRoomTypesQuery(ACTOR_ID, HOTEL_ID)))
          .willReturn(
              List.of(
                  new ListManagementRoomTypesResult(
                      ROOM_TYPE_ID,
                      "Deluxe King",
                      null,
                      (short) 2,
                      (short) 1,
                      false,
                      CREATED_AT,
                      UPDATED_AT,
                      3L)));

      // When
      ResultActions response = mvc.perform(get(PATH).principal(ACTOR));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Lấy danh sách loại phòng quản lý thành công."))
          .andExpect(jsonPath("$.data", hasSize(1)))
          .andExpect(jsonPath("$.data[0].*", hasSize(9)))
          .andExpect(jsonPath("$.data[0].id").value(ROOM_TYPE_ID.toString()))
          .andExpect(jsonPath("$.data[0].name").value("Deluxe King"))
          .andExpect(jsonPath("$.data[0].description").isEmpty())
          .andExpect(jsonPath("$.data[0].maxAdults").value(2))
          .andExpect(jsonPath("$.data[0].maxChildren").value(1))
          .andExpect(jsonPath("$.data[0].active").value(false))
          .andExpect(jsonPath("$.data[0].createdAt").value("2026-11-04T01:02:03Z"))
          .andExpect(jsonPath("$.data[0].updatedAt").value("2026-11-04T04:05:06Z"))
          .andExpect(jsonPath("$.data[0].version").value(3))
          .andExpect(jsonPath("$.data[0].hotelId").doesNotExist());
    }

    @Test
    void shouldReturnProblemDetailWhenHotelIsNotManagedByActor() throws Exception {
      // Given
      given(
              listManagementRoomTypesUseCase.execute(
                  new ListManagementRoomTypesQuery(ACTOR_ID, HOTEL_ID)))
          .willThrow(new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

      // When
      ResultActions response = mvc.perform(get(PATH).principal(ACTOR));

      // Then
      response
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"))
          .andExpect(jsonPath("$.instance").value(PATH));
    }

    @Test
    void shouldReturnProblemDetailWhenAccessIsDenied() throws Exception {
      // Given
      given(
              listManagementRoomTypesUseCase.execute(
                  new ListManagementRoomTypesQuery(ACTOR_ID, HOTEL_ID)))
          .willThrow(new HotelException(HotelErrorCode.ACCESS_DENIED));

      // When
      ResultActions response = mvc.perform(get(PATH).principal(ACTOR));

      // Then
      response
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
  }
}

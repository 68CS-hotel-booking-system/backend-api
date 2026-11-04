package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomsUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.config.PaginationProperties;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.PaginationSupport;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response danh sách phòng vật lý quản lý")
class ManagementRoomControllerResponseTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b");
  private static final UUID ROOM_ID = UUID.fromString("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
  private static final String PATH = "/api/v1/management/hotels/" + HOTEL_ID + "/rooms";
  private static final Instant AT = Instant.parse("2026-11-04T01:02:03Z");
  private static final Principal ACTOR = () -> ACTOR_ID.toString();

  @Mock private CreateRoomUseCase createRoomUseCase;

  @Mock private ListManagementRoomsUseCase listManagementRoomsUseCase;

  @Mock private UpdateRoomUseCase updateRoomUseCase;

  private MockMvc mvc;
  private ListManagementRoomsResult room;

  @BeforeEach
  void setUp() {
    // PaginationSupport là logic thật (mặc định 20, tối đa 100), không mock.
    ManagementRoomController controller =
        new ManagementRoomController(
            createRoomUseCase,
            updateRoomUseCase,
            listManagementRoomsUseCase,
            new PaginationSupport(new PaginationProperties(20, 100)));
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
    room =
        new ListManagementRoomsResult(
            ROOM_ID, ROOM_TYPE_ID, "101", null, "READY", AT, AT, null, AT, AT, 2L);
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/rooms")
  class ListManagementRoomsApiTests {

    @Test
    void shouldReturnPagedRoomsWithDefaultPaginationWhenNoQueryIsProvided() throws Exception {
      // Given
      given(
              listManagementRoomsUseCase.execute(
                  new ListManagementRoomsQuery(ACTOR_ID, HOTEL_ID, null, new PageQuery(0, 20))))
          .willReturn(new PageResult<>(List.of(room), 0, 20, 1, 1, true, true));

      // When
      ResultActions response = mvc.perform(get(PATH).principal(ACTOR));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Lấy danh sách phòng quản lý thành công."))
          .andExpect(jsonPath("$.data.page").value(0))
          .andExpect(jsonPath("$.data.size").value(20))
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.totalPages").value(1))
          .andExpect(jsonPath("$.data.first").value(true))
          .andExpect(jsonPath("$.data.last").value(true))
          .andExpect(jsonPath("$.data.content", hasSize(1)))
          .andExpect(jsonPath("$.data.content[0].*", hasSize(11)))
          .andExpect(jsonPath("$.data.content[0].id").value(ROOM_ID.toString()))
          .andExpect(jsonPath("$.data.content[0].roomTypeId").value(ROOM_TYPE_ID.toString()))
          .andExpect(jsonPath("$.data.content[0].roomNumber").value("101"))
          .andExpect(jsonPath("$.data.content[0].floor").isEmpty())
          .andExpect(jsonPath("$.data.content[0].readiness").value("READY"))
          .andExpect(jsonPath("$.data.content[0].readinessUpdatedAt").value("2026-11-04T01:02:03Z"))
          .andExpect(jsonPath("$.data.content[0].operationalFrom").value("2026-11-04T01:02:03Z"))
          .andExpect(jsonPath("$.data.content[0].operationalUntil").isEmpty())
          .andExpect(jsonPath("$.data.content[0].version").value(2))
          .andExpect(jsonPath("$.data.content[0].readinessUpdatedBy").doesNotExist())
          .andExpect(jsonPath("$.data.content[0].hotelId").doesNotExist());
    }

    @Test
    void shouldPassRoomTypeFilterAndPaginationWhenProvided() throws Exception {
      // Given
      ListManagementRoomsQuery expected =
          new ListManagementRoomsQuery(ACTOR_ID, HOTEL_ID, ROOM_TYPE_ID, new PageQuery(2, 5));
      given(listManagementRoomsUseCase.execute(expected))
          .willReturn(new PageResult<>(List.of(), 2, 5, 0, 0, false, true));

      // When
      ResultActions response =
          mvc.perform(
              get(PATH)
                  .param("roomTypeId", ROOM_TYPE_ID.toString())
                  .param("page", "2")
                  .param("size", "5")
                  .principal(ACTOR));

      // Then
      response.andExpect(status().isOk()).andExpect(jsonPath("$.data.content", hasSize(0)));
      then(listManagementRoomsUseCase).should().execute(expected);
    }

    @Test
    void shouldReturnBadRequestWhenRoomTypeIdIsNotUuid() throws Exception {
      // When
      ResultActions response =
          mvc.perform(get(PATH).param("roomTypeId", "not-a-uuid").principal(ACTOR));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
      then(listManagementRoomsUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenPageSizeExceedsLimit() throws Exception {
      // When
      ResultActions response = mvc.perform(get(PATH).param("size", "101").principal(ACTOR));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
      then(listManagementRoomsUseCase).shouldHaveNoInteractions();
    }
  }
}

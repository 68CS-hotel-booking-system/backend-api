package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.command.ReplaceHotelAmenitiesCommand;
import com.team.hotelbooking.hotel.application.command.ReplaceRoomTypeAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ReplaceHotelAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response thay danh sách tiện ích")
class ManagementAmenityControllerResponseTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b");
  private static final UUID POOL_ID = UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3");
  private static final UUID WIFI_ID = UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a");
  private static final String HOTEL_PATH = "/api/v1/management/hotels/" + HOTEL_ID + "/amenities";
  private static final String ROOM_TYPE_PATH =
      "/api/v1/management/hotels/" + HOTEL_ID + "/room-types/" + ROOM_TYPE_ID + "/amenities";
  // Principal mang authority giống JWT thật; controller suy ra cờ admin từ ROLE_ADMIN.
  private static final Authentication ACTOR =
      new UsernamePasswordAuthenticationToken(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_HOTEL_OWNER")));
  private static final Authentication ADMIN =
      new UsernamePasswordAuthenticationToken(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  @Mock private ReplaceHotelAmenitiesUseCase replaceHotelAmenitiesUseCase;

  @Mock private ReplaceRoomTypeAmenitiesUseCase replaceRoomTypeAmenitiesUseCase;

  @InjectMocks private ManagementAmenityController controller;

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
  @DisplayName("PUT /api/v1/management/hotels/{hotelId}/amenities")
  class ReplaceHotelAmenitiesApiTests {

    @Test
    void shouldReturnAssignedAmenitiesWhenListIsReplaced() throws Exception {
      // Given
      given(
              replaceHotelAmenitiesUseCase.execute(
                  new ReplaceHotelAmenitiesCommand(ACTOR_ID, false, HOTEL_ID, List.of(POOL_ID))))
          .willReturn(
              List.of(new ListAmenitiesResult(POOL_ID, "SWIMMING_POOL", "Hồ bơi", "HOTEL")));

      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[\"" + POOL_ID + "\"]}"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.*", hasSize(2)))
          .andExpect(jsonPath("$.message").value("Cập nhật tiện ích khách sạn thành công."))
          .andExpect(jsonPath("$.data", hasSize(1)))
          .andExpect(jsonPath("$.data[0].*", hasSize(4)))
          .andExpect(jsonPath("$.data[0].id").value(POOL_ID.toString()))
          .andExpect(jsonPath("$.data[0].code").value("SWIMMING_POOL"))
          .andExpect(jsonPath("$.data[0].name").value("Hồ bơi"))
          .andExpect(jsonPath("$.data[0].category").value("HOTEL"));
    }

    @Test
    void shouldReturnEmptyArrayWhenAllAmenitiesAreRemoved() throws Exception {
      // Given
      given(
              replaceHotelAmenitiesUseCase.execute(
                  new ReplaceHotelAmenitiesCommand(ACTOR_ID, false, HOTEL_ID, List.of())))
          .willReturn(List.of());

      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[]}"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void shouldReturnValidationProblemWithoutCallingUseCaseWhenAmenityIdsAreMissing()
        throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[0].field").value("amenityIds"))
          .andExpect(jsonPath("$.errors[0].message").value("Danh sách tiện ích là bắt buộc."));
      then(replaceHotelAmenitiesUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnValidationProblemWhenAmenityIdIsNull() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[null]}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[0].message").value("Mã tiện ích không được để trống."));
      then(replaceHotelAmenitiesUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenAmenityIdIsNotUuid() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[\"not-a-uuid\"]}"));

      // Then
      response.andExpect(status().isBadRequest());
      then(replaceHotelAmenitiesUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenAmenityCategoryIsWrong() throws Exception {
      // Given
      given(replaceHotelAmenitiesUseCase.execute(any(ReplaceHotelAmenitiesCommand.class)))
          .willThrow(
              new HotelException(
                  HotelErrorCode.AMENITY_CATEGORY_MISMATCH,
                  "Khách sạn chỉ được gắn tiện ích thuộc nhóm HOTEL."));

      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[\"" + WIFI_ID + "\"]}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.code").value("AMENITY_CATEGORY_MISMATCH"))
          .andExpect(
              jsonPath("$.detail").value("Khách sạn chỉ được gắn tiện ích thuộc nhóm HOTEL."))
          .andExpect(jsonPath("$.instance").value(HOTEL_PATH));
    }

    @Test
    void shouldPassAdminFlagFromPrincipalWhenActorHasAdminRole() throws Exception {
      // Given
      given(
              replaceHotelAmenitiesUseCase.execute(
                  new ReplaceHotelAmenitiesCommand(ACTOR_ID, true, HOTEL_ID, List.of())))
          .willReturn(List.of());

      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[]}"));

      // Then
      // Stub chỉ khớp khi command mang admin=true.
      response.andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void shouldReturnForbiddenWhenActorIsReceptionistOfHotel() throws Exception {
      // Given
      given(replaceHotelAmenitiesUseCase.execute(any(ReplaceHotelAmenitiesCommand.class)))
          .willThrow(new HotelException(HotelErrorCode.ACCESS_DENIED));

      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[]}"));

      // Then
      response
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnNotFoundWhenAmenityDoesNotExist() throws Exception {
      // Given
      given(replaceHotelAmenitiesUseCase.execute(any(ReplaceHotelAmenitiesCommand.class)))
          .willThrow(new HotelException(HotelErrorCode.AMENITY_NOT_FOUND));

      // When
      ResultActions response =
          mvc.perform(
              put(HOTEL_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[\"" + POOL_ID + "\"]}"));

      // Then
      response
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("AMENITY_NOT_FOUND"))
          .andExpect(jsonPath("$.detail").value("Không tìm thấy tiện ích."));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/amenities")
  class ReplaceRoomTypeAmenitiesApiTests {

    @Test
    void shouldReturnAssignedAmenitiesWhenListIsReplaced() throws Exception {
      // Given
      given(
              replaceRoomTypeAmenitiesUseCase.execute(
                  new ReplaceRoomTypeAmenitiesCommand(
                      ACTOR_ID, false, HOTEL_ID, ROOM_TYPE_ID, List.of(WIFI_ID))))
          .willReturn(
              List.of(new ListAmenitiesResult(WIFI_ID, "FREE_WIFI", "Wi-Fi miễn phí", "ROOM")));

      // When
      ResultActions response =
          mvc.perform(
              put(ROOM_TYPE_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[\"" + WIFI_ID + "\"]}"));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Cập nhật tiện ích loại phòng thành công."))
          .andExpect(jsonPath("$.data", hasSize(1)))
          .andExpect(jsonPath("$.data[0].id").value(WIFI_ID.toString()))
          .andExpect(jsonPath("$.data[0].category").value("ROOM"));
    }

    @Test
    void shouldReturnNotFoundWhenRoomTypeIsNotInHotel() throws Exception {
      // Given
      given(replaceRoomTypeAmenitiesUseCase.execute(any(ReplaceRoomTypeAmenitiesCommand.class)))
          .willThrow(new HotelException(HotelErrorCode.ROOM_TYPE_NOT_FOUND));

      // When
      ResultActions response =
          mvc.perform(
              put(ROOM_TYPE_PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[]}"));

      // Then
      response
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROOM_TYPE_NOT_FOUND"))
          .andExpect(jsonPath("$.instance").value(ROOM_TYPE_PATH));
    }

    @Test
    void shouldReturnBadRequestWhenRoomTypeIdIsNotUuid() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              put("/api/v1/management/hotels/" + HOTEL_ID + "/room-types/not-a-uuid/amenities")
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amenityIds\":[]}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
      then(replaceRoomTypeAmenitiesUseCase).shouldHaveNoInteractions();
    }
  }
}

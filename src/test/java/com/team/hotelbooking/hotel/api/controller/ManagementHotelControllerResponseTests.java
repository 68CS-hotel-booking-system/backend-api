package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.command.UpdateHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.PaginationSupport;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response sửa thông tin khách sạn")
class ManagementHotelControllerResponseTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID CITY_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String PATH = "/api/v1/management/hotels/" + HOTEL_ID;
  // Principal mang authority giống JWT thật (ROLE_*); controller suy ra cờ admin từ đây.
  private static final Authentication ACTOR =
      new UsernamePasswordAuthenticationToken(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_HOTEL_OWNER")));
  private static final Authentication ADMIN =
      new UsernamePasswordAuthenticationToken(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  @Mock private GetManagementHotelUseCase getManagementHotelUseCase;

  @Mock private ListManagementHotelsUseCase listManagementHotelsUseCase;

  @Mock private UpdateHotelUseCase updateHotelUseCase;

  @Mock private PaginationSupport paginationSupport;

  @InjectMocks private ManagementHotelController controller;

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
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}")
  class UpdateHotelApiTests {

    @Test
    void shouldReturnUpdatedHotelWithNewVersionWhenRequestIsValid() throws Exception {
      // Given
      UpdateHotelCommand expected =
          new UpdateHotelCommand(
              ACTOR_ID,
              false,
              HOTEL_ID,
              "Khách sạn Biển Xanh Mới",
              null,
              null,
              null,
              null,
              new BigDecimal("16.0614"),
              new BigDecimal("108.2478"),
              (short) 5,
              LocalTime.of(13, 30),
              null,
              3L);
      given(updateHotelUseCase.execute(expected))
          .willReturn(
              new ManagementHotelResult(
                  HOTEL_ID,
                  ACTOR_ID,
                  CITY_ID,
                  "Khách sạn Biển Xanh Mới",
                  "khach-san-bien-xanh",
                  null,
                  "12 Võ Nguyên Giáp",
                  null,
                  null,
                  new BigDecimal("16.0614"),
                  null,
                  (short) 5,
                  LocalTime.of(13, 30),
                  LocalTime.of(12, 0),
                  "ACTIVE",
                  Instant.parse("2026-11-04T01:02:03Z"),
                  Instant.parse("2026-11-09T04:05:06Z"),
                  4L));

      // When
      // Tọa độ gửi kèm số 0 thừa: request chuẩn hóa như API tạo khách sạn.
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"name":"Khách sạn Biển Xanh Mới","latitude":16.061400,
                       "longitude":108.247800,"starRating":5,"checkInTime":"13:30:00",
                       "version":3}
                      """));

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.*", hasSize(2)))
          .andExpect(jsonPath("$.message").value("Cập nhật thông tin khách sạn thành công."))
          .andExpect(jsonPath("$.data.*", hasSize(18)))
          .andExpect(jsonPath("$.data.contactPhone").isEmpty())
          .andExpect(jsonPath("$.data.contactEmail").isEmpty())
          .andExpect(jsonPath("$.data.id").value(HOTEL_ID.toString()))
          .andExpect(jsonPath("$.data.name").value("Khách sạn Biển Xanh Mới"))
          .andExpect(jsonPath("$.data.slug").value("khach-san-bien-xanh"))
          .andExpect(jsonPath("$.data.description").isEmpty())
          .andExpect(jsonPath("$.data.latitude").value(16.0614))
          .andExpect(jsonPath("$.data.longitude").isEmpty())
          .andExpect(jsonPath("$.data.starRating").value(5))
          .andExpect(jsonPath("$.data.checkInTime").value("13:30:00"))
          .andExpect(jsonPath("$.data.checkOutTime").value("12:00:00"))
          .andExpect(jsonPath("$.data.status").value("ACTIVE"))
          .andExpect(jsonPath("$.data.createdAt").value("2026-11-04T01:02:03Z"))
          .andExpect(jsonPath("$.data.updatedAt").value("2026-11-09T04:05:06Z"))
          .andExpect(jsonPath("$.data.version").value(4));
    }

    @Test
    void shouldPassAdminFlagFromPrincipalWhenActorHasAdminRole() throws Exception {
      // Given
      UpdateHotelCommand expected =
          new UpdateHotelCommand(
              ACTOR_ID, true, HOTEL_ID, "Tên mới", null, null, null, null, null, null, null, null,
              null, 3L);
      given(updateHotelUseCase.execute(expected))
          .willThrow(new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Tên mới\",\"version\":3}"));

      // Then
      // Stub chỉ khớp khi command mang admin=true; lỗi 404 chứng minh controller đã truyền đúng.
      response
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldReturnForbiddenWhenActorIsReceptionistOfHotel() throws Exception {
      // Given
      given(updateHotelUseCase.execute(any(UpdateHotelCommand.class)))
          .willThrow(new HotelException(HotelErrorCode.ACCESS_DENIED));

      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Tên mới\",\"version\":3}"));

      // Then
      response
          .andExpect(status().isForbidden())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
          .andExpect(jsonPath("$.detail").value("Bạn không có quyền thực hiện thao tác này."));
    }

    @Test
    void shouldReturnValidationProblemInsteadOfClearingWhenNameIsBlank() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"   \",\"version\":3}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors", hasSize(1)))
          .andExpect(jsonPath("$.errors[0].field").value("name"))
          .andExpect(jsonPath("$.errors[0].message").value("Tên khách sạn không được để trống."));
      then(updateHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnValidationProblemInsteadOfClearingWhenDescriptionIsEmpty() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"description\":\"\",\"version\":3}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[0].field").value("description"))
          .andExpect(jsonPath("$.errors[0].message").value("Mô tả khách sạn không được để trống."));
      then(updateHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnValidationProblemWhenLatitudeIsOutOfRange() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"latitude\":91,\"longitude\":100,\"version\":3}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("latitude"))
          .andExpect(jsonPath("$.errors[0].message").value("Vĩ độ phải nhỏ hơn hoặc bằng 90."));
      then(updateHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnValidationProblemWithoutCallingUseCaseWhenVersionIsMissing() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Tên mới\"}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[0].field").value("version"))
          .andExpect(jsonPath("$.errors[0].message").value("Version của khách sạn là bắt buộc."));
      then(updateHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnValidationProblemWhenStarRatingIsOutOfRange() throws Exception {
      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"starRating\":6,\"version\":3}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[0].field").value("starRating"))
          .andExpect(jsonPath("$.errors[0].message").value("Số sao khách sạn phải từ 1 đến 5."));
      then(updateHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnConflictWhenVersionIsStale() throws Exception {
      // Given
      given(updateHotelUseCase.execute(any(UpdateHotelCommand.class)))
          .willThrow(new ObjectOptimisticLockingFailureException(Object.class, HOTEL_ID));

      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Tên mới\",\"version\":2}"));

      // Then
      response
          .andExpect(status().isConflict())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"))
          .andExpect(
              jsonPath("$.detail")
                  .value(
                      "Dữ liệu đã được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại."))
          .andExpect(jsonPath("$.instance").value(PATH));
    }

    @Test
    void shouldReturnBadRequestWhenNoFieldIsProvided() throws Exception {
      // Given
      given(updateHotelUseCase.execute(any(UpdateHotelCommand.class)))
          .willThrow(
              new HotelException(
                  HotelErrorCode.INVALID_REQUEST,
                  "Phải có ít nhất một thông tin khách sạn cần cập nhật."));

      // When
      ResultActions response =
          mvc.perform(
              patch(PATH)
                  .principal(ACTOR)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"version\":3}"));

      // Then
      response
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
          .andExpect(
              jsonPath("$.detail").value("Phải có ít nhất một thông tin khách sạn cần cập nhật."));
    }
  }
}

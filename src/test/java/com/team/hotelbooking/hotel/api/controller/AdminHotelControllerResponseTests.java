package com.team.hotelbooking.hotel.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.HotelPublishRequirement;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.Instant;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response API duyệt khách sạn")
class AdminHotelControllerResponseTests {

  private static final UUID ADMIN_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final String APPROVE_PATH = "/api/v1/management/hotels/" + HOTEL_ID + "/approve";
  private static final Authentication ADMIN =
      new UsernamePasswordAuthenticationToken(
          ADMIN_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  @Mock private CreateHotelUseCase createHotelUseCase;
  @Mock private ApproveHotelUseCase approveHotelUseCase;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();
    new JacksonConfig().jsonCustomizer().customize(mapperBuilder);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminHotelController(createHotelUseCase, approveHotelUseCase))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(mapperBuilder))
            .setControllerAdvice(
                new HotelExceptionHandler(new ProblemDetailFactory()),
                new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}/approve")
  class ApproveApiTests {

    @Test
    @DisplayName("Giữ message và dạng response của develop khi duyệt thành công")
    void shouldReturnActiveHotelWhenApproving() throws Exception {
      // Given
      given(approveHotelUseCase.execute(HOTEL_ID, ADMIN_ID)).willReturn(result());

      // When / Then
      mvc.perform(patch(APPROVE_PATH).principal(ADMIN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Duyệt khách sạn thành công."))
          .andExpect(jsonPath("$.data.id").value(HOTEL_ID.toString()))
          .andExpect(jsonPath("$.data.status").value("ACTIVE"))
          .andExpect(jsonPath("$.data.updatedAt").value("2026-11-09T08:00:00Z"));
    }

    @Test
    @DisplayName("Trả 422 HOTEL_NOT_READY liệt kê từng điều kiện HT-03 còn thiếu")
    void shouldReturnUnprocessableWithEveryReasonWhenHotelIsNotReady() throws Exception {
      // Given
      given(approveHotelUseCase.execute(HOTEL_ID, ADMIN_ID))
          .willThrow(
              HotelException.notReady(
                  List.of(
                      HotelPublishRequirement.CONTACT,
                      HotelPublishRequirement.ROOM_TYPE,
                      HotelPublishRequirement.OPERATIONAL_ROOM)));

      // When / Then
      mvc.perform(patch(APPROVE_PATH).principal(ADMIN))
          .andExpect(status().is(422))
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_READY"))
          .andExpect(jsonPath("$.detail").value("Khách sạn chưa đủ điều kiện mở bán."))
          .andExpect(jsonPath("$.errors", hasSize(3)))
          .andExpect(jsonPath("$.errors[0].field").value("contact"))
          .andExpect(
              jsonPath("$.errors[0].message")
                  .value("Khách sạn chưa có số điện thoại hoặc email liên hệ."))
          .andExpect(jsonPath("$.errors[1].field").value("roomTypes"))
          .andExpect(jsonPath("$.errors[2].field").value("rooms"));
    }

    @Test
    @DisplayName("Giữ 400 VALIDATION_FAILED của develop khi khách sạn không còn DRAFT")
    void shouldReturnBadRequestWhenHotelIsNotDraft() throws Exception {
      // Given
      given(approveHotelUseCase.execute(HOTEL_ID, ADMIN_ID))
          .willThrow(
              new HotelException(
                  HotelErrorCode.VALIDATION_FAILED,
                  "Chỉ khách sạn ở trạng thái DRAFT mới có thể kích hoạt."));

      // When / Then
      mvc.perform(patch(APPROVE_PATH).principal(ADMIN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(
              jsonPath("$.detail").value("Chỉ khách sạn ở trạng thái DRAFT mới có thể kích hoạt."));
    }
  }

  private static ManagementHotelResult result() {
    return new ManagementHotelResult(
        HOTEL_ID,
        UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9"),
        UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3"),
        "Khách sạn Biển Xanh",
        "khach-san-bien-xanh",
        null,
        "12 Võ Nguyên Giáp",
        "+84905123456",
        null,
        null,
        null,
        null,
        LocalTime.of(14, 0),
        LocalTime.of(12, 0),
        "ACTIVE",
        Instant.parse("2026-11-01T00:00:00Z"),
        Instant.parse("2026-11-09T08:00:00Z"),
        3L);
  }
}

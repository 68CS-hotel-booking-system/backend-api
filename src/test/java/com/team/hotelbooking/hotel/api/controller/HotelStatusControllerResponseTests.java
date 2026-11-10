package com.team.hotelbooking.hotel.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.hotel.api.exception.HotelExceptionHandler;
import com.team.hotelbooking.hotel.application.command.LockHotelCommand;
import com.team.hotelbooking.hotel.application.command.ResumeHotelCommand;
import com.team.hotelbooking.hotel.application.command.SuspendHotelCommand;
import com.team.hotelbooking.hotel.application.command.UnlockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.LockHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ResumeHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.SuspendHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.UnlockHotelUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response API vòng đời bán khách sạn")
class HotelStatusControllerResponseTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final String ADMIN_PATH = "/api/v1/admin/hotels/" + HOTEL_ID;
  private static final String MANAGEMENT_PATH = "/api/v1/management/hotels/" + HOTEL_ID;
  private static final Authentication ADMIN =
      new UsernamePasswordAuthenticationToken(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  private static final Authentication OWNER =
      new UsernamePasswordAuthenticationToken(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_HOTEL_OWNER")));

  @Mock private LockHotelUseCase lockHotelUseCase;
  @Mock private UnlockHotelUseCase unlockHotelUseCase;
  @Mock private SuspendHotelUseCase suspendHotelUseCase;
  @Mock private ResumeHotelUseCase resumeHotelUseCase;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();
    new JacksonConfig().jsonCustomizer().customize(mapperBuilder);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminHotelStatusController(lockHotelUseCase, unlockHotelUseCase),
                new ManagementHotelStatusController(suspendHotelUseCase, resumeHotelUseCase))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(mapperBuilder))
            .setControllerAdvice(
                new HotelExceptionHandler(new ProblemDetailFactory()),
                new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  private static ManagementHotelResult result(String status) {
    return new ManagementHotelResult(
        HOTEL_ID,
        ACTOR_ID,
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
        status,
        Instant.parse("2026-11-01T00:00:00Z"),
        Instant.parse("2026-11-09T08:00:00Z"),
        3L);
  }

  @Nested
  @DisplayName("POST /api/v1/admin/hotels/{hotelId}/lock và /unlock")
  class LockApiTests {

    @Test
    void shouldPassTrimmedReasonToUseCaseWhenLocking() throws Exception {
      // Given
      given(lockHotelUseCase.execute(any())).willReturn(result("LOCKED"));

      // When
      mvc.perform(
              post(ADMIN_PATH + "/lock")
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\": \"Vi phạm quy định\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Khóa bán khách sạn thành công."))
          .andExpect(jsonPath("$.data.status").value("LOCKED"));

      // Then
      then(lockHotelUseCase)
          .should()
          .execute(new LockHotelCommand(ACTOR_ID, HOTEL_ID, "Vi phạm quy định"));
    }

    @Test
    void shouldReturnBadRequestWithoutCallingUseCaseWhenLockReasonIsBlank() throws Exception {
      // When / Then
      mvc.perform(
              post(ADMIN_PATH + "/lock")
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\": \"   \"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("reason"))
          .andExpect(jsonPath("$.errors[0].message").value("Phải nhập lý do khóa bán."));
      then(lockHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnSuspendedHotelWhenUnlocking() throws Exception {
      // Given
      given(
              unlockHotelUseCase.execute(
                  new UnlockHotelCommand(ACTOR_ID, HOTEL_ID, "Đã xử lý vi phạm")))
          .willReturn(result("SUSPENDED"));

      // When / Then
      mvc.perform(
              post(ADMIN_PATH + "/unlock")
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\": \"Đã xử lý vi phạm\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Gỡ khóa bán khách sạn thành công."))
          .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void shouldReturnBadRequestWhenUnlockBodyIsMissing() throws Exception {
      // When / Then
      mvc.perform(post(ADMIN_PATH + "/unlock").principal(ADMIN)).andExpect(status().isBadRequest());
      then(unlockHotelUseCase).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("POST /api/v1/management/hotels/{hotelId}/suspend và /resume")
  class ManagementApiTests {

    @Test
    void shouldPassActorAndNonAdminFlagWhenOwnerSuspends() throws Exception {
      // Given
      given(
              suspendHotelUseCase.execute(
                  new SuspendHotelCommand(ACTOR_ID, false, HOTEL_ID, "Sửa hồ bơi")))
          .willReturn(result("SUSPENDED"));

      // When / Then
      mvc.perform(
              post(MANAGEMENT_PATH + "/suspend")
                  .principal(OWNER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\": \"Sửa hồ bơi\"}"))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Tạm ngừng bán khách sạn thành công."))
          .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void shouldReturnConflictWithLockedCodeWhenManagerResumesLockedHotel() throws Exception {
      // Given
      given(resumeHotelUseCase.execute(new ResumeHotelCommand(ACTOR_ID, false, HOTEL_ID, null)))
          .willThrow(new HotelException(HotelErrorCode.HOTEL_LOCKED));

      // When / Then
      mvc.perform(post(MANAGEMENT_PATH + "/resume").principal(OWNER))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("HOTEL_LOCKED"))
          .andExpect(
              jsonPath("$.detail")
                  .value(
                      "Khách sạn đang bị quản trị hệ thống khóa bán; chỉ quản trị hệ thống được"
                          + " gỡ khóa."));
    }

    @Test
    void shouldReturnBadRequestWhenReasonIsLongerThanLimit() throws Exception {
      // When / Then
      mvc.perform(
              post(MANAGEMENT_PATH + "/suspend")
                  .principal(OWNER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\": \"" + "x".repeat(501) + "\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].message").value("Lý do không được vượt quá 500 ký tự."));
      then(suspendHotelUseCase).shouldHaveNoInteractions();
    }
  }
}

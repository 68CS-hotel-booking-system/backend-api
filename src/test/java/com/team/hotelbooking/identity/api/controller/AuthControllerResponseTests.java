package com.team.hotelbooking.identity.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.identity.api.AuthCookieManager;
import com.team.hotelbooking.identity.api.dto.request.LoginRequest;
import com.team.hotelbooking.identity.api.dto.request.RefreshTokenRequest;
import com.team.hotelbooking.identity.api.dto.response.LoginResponse;
import com.team.hotelbooking.identity.api.dto.response.VerifyEmailResponse;
import com.team.hotelbooking.identity.api.exception.IdentityExceptionHandler;
import com.team.hotelbooking.identity.application.command.LoginCommand;
import com.team.hotelbooking.identity.application.command.RegisterCommand;
import com.team.hotelbooking.identity.application.result.ForgotPasswordResult;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.result.RegisterResult;
import com.team.hotelbooking.identity.application.result.VerifyEmailResult;
import com.team.hotelbooking.identity.application.usecase.ForgotPasswordUseCase;
import com.team.hotelbooking.identity.application.usecase.LoginUseCase;
import com.team.hotelbooking.identity.application.usecase.LogoutUseCase;
import com.team.hotelbooking.identity.application.usecase.RefreshTokenUseCase;
import com.team.hotelbooking.identity.application.usecase.RegisterUseCase;
import com.team.hotelbooking.identity.application.usecase.ResetPasswordUseCase;
import com.team.hotelbooking.identity.application.usecase.VerifyEmailUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.exception.CommonErrorCode;
import com.team.hotelbooking.shared.exception.ErrorCode;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response của AuthController")
class AuthControllerResponseTests {

  private static final UUID USER_ID = UUID.fromString("23c53d62-03e2-48f3-bd5e-33d51d31e3fd");
  private static final String EMAIL = "user@example.com";
  private static final String PASSWORD = "long-enough-password";
  private static final String FULL_NAME = "Nguyễn Văn A";
  private static final String PHONE = "+84912345678";
  private static final String ACCESS_TOKEN = "access.\"sensitive-token";
  private static final String REFRESH_TOKEN = "refresh\\sensitive-token";
  private static final String INCOMING_TOKEN = "incoming-token";
  private static final String VERIFICATION_TOKEN = "verification-token";
  private static final Instant EXPIRES_AT = Instant.parse("2026-11-02T01:02:03.123456789Z");
  private static final String API_RESPONSE_PACKAGE =
      "com.team.hotelbooking.identity.api.dto.response";
  private static final String REGISTER_REQUEST_JSON =
      """
      {
        "email": "user@example.com",
        "password": "long-enough-password",
        "fullName": "Nguyễn Văn A",
        "phone": "+84912345678"
      }
      """;
  private static final String LOGIN_REQUEST_JSON =
      """
      {
        "email": "user@example.com",
        "password": "long-enough-password"
      }
      """;
  private static final String TOKEN_REQUEST_JSON = "{\"refreshToken\":\"incoming-token\"}";
  private static final String VERIFY_REQUEST_JSON = "{\"token\":\"verification-token\"}";
  private static final String EMAIL_REQUEST_JSON = "{\"email\":\"user@example.com\"}";

  @Mock private RegisterUseCase registerUseCase;

  @Mock private VerifyEmailUseCase verifyEmailUseCase;

  @Mock private LoginUseCase loginUseCase;

  @Mock private RefreshTokenUseCase refreshTokenUseCase;

  @Mock private LogoutUseCase logoutUseCase;

  @Mock private ForgotPasswordUseCase forgotPasswordUseCase;

  @Mock private ResetPasswordUseCase resetPasswordUseCase;

  @Mock private AuthCookieManager authCookieManager;

  @InjectMocks private AuthController controller;

  private RegisterCommand registerCommand;
  private LoginCommand loginCommand;
  private RegisterResult registerResult;
  private LoginResult loginResult;
  private MockMvc mvc;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    registerCommand = new RegisterCommand(EMAIL, PASSWORD, FULL_NAME, PHONE);
    loginCommand = new LoginCommand(EMAIL, PASSWORD);
    registerResult = new RegisterResult(USER_ID, EMAIL, FULL_NAME, PHONE, "pending", false, 7L);
    loginResult =
        loginResultWithUser(
            new LoginResult.UserInfo(USER_ID, EMAIL, FULL_NAME, PHONE, "active", true, 9L));

    JsonMapper.Builder mapperBuilder = JsonMapper.builder();
    new JacksonConfig().jsonCustomizer().customize(mapperBuilder);
    JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(mapperBuilder);
    mapper = converter.getMapper();
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .setControllerAdvice(
                new IdentityExceptionHandler(new ProblemDetailFactory()),
                new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Nested
  @DisplayName("POST /api/v1/auth/register")
  class RegisterApiTests {

    @Test
    void shouldExposeRegisterResponseWhenInspectingContract() {
      // Given
      String methodName = "register";

      // When
      Class<?> actualType = responseType(methodName);

      // Then
      assertEquals(API_RESPONSE_PACKAGE + ".RegisterResponse", actualType.getName());
    }

    @Test
    void shouldPreserveJsonAndDefaultHeadersWhenRegistrationSucceeds() throws Exception {
      // Given
      given(registerUseCase.execute(registerCommand)).willReturn(registerResult);

      // When
      ResultActions response = performPost("/api/v1/auth/register", REGISTER_REQUEST_JSON);

      // Then
      response
          .andExpect(status().isCreated())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(header().doesNotExist("Pragma"));
      assertJsonResponse(ApiResponse.success("Đăng ký thành công.", registerResult), response);
    }

    @Test
    void shouldReturnProblemDetailWhenEmailIsAlreadyUsed() throws Exception {
      // Given
      IdentityException error = new IdentityException(IdentityErrorCode.EMAIL_ALREADY_USED);
      given(registerUseCase.execute(registerCommand)).willThrow(error);

      // When
      ResultActions response = performPost("/api/v1/auth/register", REGISTER_REQUEST_JSON);

      // Then
      assertProblemDetail(
          response, 409, IdentityErrorCode.EMAIL_ALREADY_USED, "/api/v1/auth/register");
    }

    @Test
    void shouldReturnProblemDetailWhenRegistrationFailsUnexpectedly() throws Exception {
      // Given
      given(registerUseCase.execute(registerCommand))
          .willThrow(new IllegalStateException("private connection pool closed"));

      // When
      ResultActions response = performPost("/api/v1/auth/register", REGISTER_REQUEST_JSON);

      // Then
      assertProblemDetail(response, 500, CommonErrorCode.INTERNAL_ERROR, "/api/v1/auth/register");
      response.andExpect(content().string(not(containsString("private connection pool closed"))));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/email-verifications")
  class VerifyEmailApiTests {

    @Test
    void shouldExposeVerifyEmailResponseWhenInspectingContract() {
      // Given
      String methodName = "verifyEmail";

      // When
      Class<?> actualType = responseType(methodName);

      // Then
      assertEquals(API_RESPONSE_PACKAGE + ".VerifyEmailResponse", actualType.getName());
    }

    @ParameterizedTest(name = "accepted = {0}")
    @ValueSource(booleans = {true, false})
    void shouldPreserveAcceptedValueWhenUseCaseReturnsResult(boolean accepted) throws Exception {
      // Given
      VerifyEmailResult result = new VerifyEmailResult(accepted);
      given(verifyEmailUseCase.execute(VERIFICATION_TOKEN)).willReturn(result);

      // When
      ResultActions response = performPost("/api/v1/auth/email-verifications", VERIFY_REQUEST_JSON);

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(header().doesNotExist("Cache-Control"))
          .andExpect(header().doesNotExist("Pragma"));
      assertJsonResponse(
          ApiResponse.success(
              "Xác minh email bằng token thành công.", VerifyEmailResponse.from(result)),
          response);
    }

    @ParameterizedTest(name = "{0} → HTTP {1}")
    @CsvSource({"TOKEN_INVALID,422", "TOKEN_EXPIRED,422", "TOKEN_ALREADY_USED,422"})
    void shouldReturnProblemDetailWhenVerificationFails(String code, int httpStatus)
        throws Exception {
      // Given
      IdentityErrorCode errorCode = IdentityErrorCode.valueOf(code);
      given(verifyEmailUseCase.execute(VERIFICATION_TOKEN))
          .willThrow(new IdentityException(errorCode));

      // When
      ResultActions response = performPost("/api/v1/auth/email-verifications", VERIFY_REQUEST_JSON);

      // Then
      assertProblemDetail(response, httpStatus, errorCode, "/api/v1/auth/email-verifications");
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/login")
  class LoginApiTests {

    @Test
    void shouldExposeLoginResponseWhenInspectingContract() {
      // Given
      String methodName = "login";

      // When
      Class<?> actualType = responseType(methodName);

      // Then
      assertEquals(API_RESPONSE_PACKAGE + ".LoginResponse", actualType.getName());
    }

    @Test
    void shouldExposeApiUserDtoWhenInspectingLoginResponse() {
      // Given
      Class<?> loginResponseType = responseType("login");

      // When
      Class<?> userType =
          Arrays.stream(loginResponseType.getRecordComponents())
              .filter(component -> component.getName().equals("user"))
              .findFirst()
              .orElseThrow()
              .getType();

      // Then
      assertEquals(API_RESPONSE_PACKAGE, userType.getPackageName());
    }

    @Test
    void shouldPreserveJsonAndCacheHeadersWhenLoginSucceeds() throws Exception {
      // Given
      given(loginUseCase.execute(loginCommand)).willReturn(loginResult);

      // When
      ResultActions response = performPost("/api/v1/auth/login", LOGIN_REQUEST_JSON);

      // Then
      assertTokenResponse(response, "Đăng nhập thành công.", loginResult);
    }

    @Test
    void shouldPreserveNullFieldsWhenUserFieldsAreNull() throws Exception {
      // Given
      LoginResult result =
          loginResultWithUser(
              new LoginResult.UserInfo(USER_ID, EMAIL, null, null, "active", false, 0L));
      given(loginUseCase.execute(loginCommand)).willReturn(result);

      // When
      ResultActions response = performPost("/api/v1/auth/login", LOGIN_REQUEST_JSON);

      // Then
      assertTokenResponse(response, "Đăng nhập thành công.", result);
    }

    @Test
    void shouldPreserveNullUserWhenUseCaseReturnsNullUser() throws Exception {
      // Given
      LoginResult result = loginResultWithUser(null);
      given(loginUseCase.execute(loginCommand)).willReturn(result);

      // When
      ResultActions response = performPost("/api/v1/auth/login", LOGIN_REQUEST_JSON);

      // Then
      assertTokenResponse(response, "Đăng nhập thành công.", result);
    }

    @Test
    void shouldRedactTokensWhenResponseIsConvertedToString() {
      // Given
      given(loginUseCase.execute(loginCommand)).willReturn(loginResult);
      LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

      // When
      String rendered =
          controller
              .login(request, new MockHttpServletRequest(), new MockHttpServletResponse())
              .getBody()
              .data()
              .toString();

      // Then
      assertTokensRedacted(rendered);
    }

    @ParameterizedTest(name = "{0} → HTTP {1}")
    @CsvSource({"INVALID_CREDENTIALS,401", "ACCOUNT_LOCKED,403"})
    void shouldReturnProblemDetailWhenLoginFails(String code, int httpStatus) throws Exception {
      // Given
      IdentityErrorCode errorCode = IdentityErrorCode.valueOf(code);
      given(loginUseCase.execute(loginCommand)).willThrow(new IdentityException(errorCode));

      // When
      ResultActions response = performPost("/api/v1/auth/login", LOGIN_REQUEST_JSON);

      // Then
      assertProblemDetail(response, httpStatus, errorCode, "/api/v1/auth/login");
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/refresh")
  class RefreshApiTests {

    @Test
    void shouldExposeLoginResponseWhenInspectingContract() {
      // Given
      String methodName = "refresh";

      // When
      Class<?> actualType = responseType(methodName);

      // Then
      assertEquals(API_RESPONSE_PACKAGE + ".LoginResponse", actualType.getName());
    }

    @Test
    void shouldPreserveJsonAndCacheHeadersWhenRefreshSucceeds() throws Exception {
      // Given
      given(refreshTokenUseCase.execute(INCOMING_TOKEN)).willReturn(loginResult);

      // When
      ResultActions response = performPost("/api/v1/auth/refresh", TOKEN_REQUEST_JSON);

      // Then
      assertTokenResponse(response, "Làm mới phiên đăng nhập thành công.", loginResult);
    }

    @Test
    void shouldPreserveNullFieldsWhenUserFieldsAreNull() throws Exception {
      // Given
      LoginResult result =
          loginResultWithUser(
              new LoginResult.UserInfo(USER_ID, EMAIL, null, null, "active", false, 0L));
      given(refreshTokenUseCase.execute(INCOMING_TOKEN)).willReturn(result);

      // When
      ResultActions response = performPost("/api/v1/auth/refresh", TOKEN_REQUEST_JSON);

      // Then
      assertTokenResponse(response, "Làm mới phiên đăng nhập thành công.", result);
    }

    @Test
    void shouldPreserveNullUserWhenUseCaseReturnsNullUser() throws Exception {
      // Given
      LoginResult result = loginResultWithUser(null);
      given(refreshTokenUseCase.execute(INCOMING_TOKEN)).willReturn(result);

      // When
      ResultActions response = performPost("/api/v1/auth/refresh", TOKEN_REQUEST_JSON);

      // Then
      assertTokenResponse(response, "Làm mới phiên đăng nhập thành công.", result);
    }

    @Test
    void shouldRedactTokensWhenResponseIsConvertedToString() {
      // Given
      given(refreshTokenUseCase.execute(INCOMING_TOKEN)).willReturn(loginResult);
      RefreshTokenRequest request = new RefreshTokenRequest(INCOMING_TOKEN);

      // When
      String rendered =
          controller
              .refresh(request, new MockHttpServletRequest(), new MockHttpServletResponse())
              .getBody()
              .data()
              .toString();

      // Then
      assertTokensRedacted(rendered);
    }

    @ParameterizedTest(name = "{0} → HTTP {1}")
    @CsvSource({"INVALID_REFRESH_TOKEN,401", "REFRESH_TOKEN_REUSED,401"})
    void shouldReturnProblemDetailWhenRefreshFails(String code, int httpStatus) throws Exception {
      // Given
      IdentityErrorCode errorCode = IdentityErrorCode.valueOf(code);
      given(refreshTokenUseCase.execute(INCOMING_TOKEN))
          .willThrow(new IdentityException(errorCode));

      // When
      ResultActions response = performPost("/api/v1/auth/refresh", TOKEN_REQUEST_JSON);

      // Then
      assertProblemDetail(response, httpStatus, errorCode, "/api/v1/auth/refresh");
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/logout")
  class LogoutApiTests {

    @Test
    void shouldPreserveVoidContractAndHeadersWhenLogoutSucceeds() throws Exception {
      // Given
      willDoNothing().given(logoutUseCase).execute(INCOMING_TOKEN);

      // When
      ResultActions response = performPost("/api/v1/auth/logout", TOKEN_REQUEST_JSON);

      // Then
      assertEquals(Void.class, responseType("logout"));
      response
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(header().doesNotExist("Pragma"));
      assertEquals(
          mapper.readTree("{\"message\":\"Đăng xuất thành công.\",\"data\":null}"),
          mapper.readTree(
              response.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldReturnProblemDetailWhenLogoutFailsUnexpectedly() throws Exception {
      // Given
      willThrow(new IllegalStateException("private storage failure"))
          .given(logoutUseCase)
          .execute(INCOMING_TOKEN);

      // When
      ResultActions response = performPost("/api/v1/auth/logout", TOKEN_REQUEST_JSON);

      // Then
      assertProblemDetail(response, 500, CommonErrorCode.INTERNAL_ERROR, "/api/v1/auth/logout");
      response.andExpect(content().string(not(containsString("private storage failure"))));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/forgot-password")
  class ForgotPasswordApiTests {

    @Test
    void shouldExposeForgotPasswordResponseWhenInspectingContract() {
      // Given
      String methodName = "forgotPassword";

      // When
      Class<?> actualType = responseType(methodName);

      // Then
      assertEquals(API_RESPONSE_PACKAGE + ".ForgotPasswordResponse", actualType.getName());
    }

    @ParameterizedTest(name = "accepted = {0}")
    @ValueSource(booleans = {true, false})
    void shouldPreserveAcceptedValueAndHeadersWhenUseCaseReturnsResult(boolean accepted)
        throws Exception {
      // Given
      ForgotPasswordResult result = new ForgotPasswordResult(accepted);
      given(forgotPasswordUseCase.execute(EMAIL)).willReturn(result);

      // When
      ResultActions response = performPost("/api/v1/auth/forgot-password", EMAIL_REQUEST_JSON);

      // Then
      response
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(header().doesNotExist("Pragma"));
      assertJsonResponse(
          ApiResponse.success(
              "Nếu tài khoản đủ điều kiện, hệ thống sẽ gửi email đặt lại mật khẩu.", result),
          response);
    }

    @Test
    void shouldReturnProblemDetailWhenForgotPasswordFailsUnexpectedly() throws Exception {
      // Given
      given(forgotPasswordUseCase.execute(EMAIL))
          .willThrow(new IllegalStateException("private storage failure"));

      // When
      ResultActions response = performPost("/api/v1/auth/forgot-password", EMAIL_REQUEST_JSON);

      // Then
      assertProblemDetail(
          response, 500, CommonErrorCode.INTERNAL_ERROR, "/api/v1/auth/forgot-password");
      response.andExpect(content().string(not(containsString("private storage failure"))));
    }
  }

  // Helper dùng chung: tạo fixture, thực hiện request và kiểm tra response.

  private LoginResult loginResultWithUser(LoginResult.UserInfo user) {
    return new LoginResult(
        ACCESS_TOKEN, REFRESH_TOKEN, "Bearer", EXPIRES_AT, user, false, EXPIRES_AT);
  }

  private Class<?> responseType(String methodName) {
    Method method =
        Arrays.stream(AuthController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();
    Class<?> type =
        ResolvableType.forMethodReturnType(method).getGeneric(0).getGeneric(0).resolve();
    assertNotNull(type);
    return type;
  }

  private ResultActions performPost(String path, String requestJson) throws Exception {
    return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(requestJson));
  }

  private void assertTokenResponse(
      ResultActions response, String message, LoginResult expectedResult) throws Exception {
    response
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("Pragma", "no-cache"))
        .andExpect(jsonPath("$.data.expiresAt").value("2026-11-02T01:02:03.123456789Z"));
    assertJsonResponse(
        ApiResponse.success(message, LoginResponse.from(expectedResult, false)), response);
  }

  private void assertTokensRedacted(String rendered) {
    assertFalse(rendered.contains(ACCESS_TOKEN));
    assertFalse(rendered.contains(REFRESH_TOKEN));
    assertTrue(rendered.contains("REDACTED"));
  }

  private void assertJsonResponse(Object expectedResponse, ResultActions response)
      throws Exception {
    assertEquals(
        mapper.readTree(mapper.writeValueAsString(expectedResponse)),
        mapper.readTree(
            response.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)));
  }

  private void assertProblemDetail(
      ResultActions response, int httpStatus, ErrorCode errorCode, String path) throws Exception {
    response
        .andExpect(status().is(httpStatus))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(httpStatus))
        .andExpect(jsonPath("$.code").value(errorCode.code()))
        .andExpect(jsonPath("$.detail").value(errorCode.defaultMessage()))
        .andExpect(jsonPath("$.instance").value(path))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.message").doesNotExist())
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}

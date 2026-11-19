package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.AuthCookieManager;
import com.team.hotelbooking.identity.api.dto.request.ForgotPasswordRequest;
import com.team.hotelbooking.identity.api.dto.request.LoginRequest;
import com.team.hotelbooking.identity.api.dto.request.LogoutRequest;
import com.team.hotelbooking.identity.api.dto.request.RefreshTokenRequest;
import com.team.hotelbooking.identity.api.dto.request.RegisterRequest;
import com.team.hotelbooking.identity.api.dto.request.ResetPasswordRequest;
import com.team.hotelbooking.identity.api.dto.request.VerifyEmailRequest;
import com.team.hotelbooking.identity.api.dto.response.ForgotPasswordResponse;
import com.team.hotelbooking.identity.api.dto.response.LoginResponse;
import com.team.hotelbooking.identity.api.dto.response.RegisterResponse;
import com.team.hotelbooking.identity.api.dto.response.ResetPasswordResponse;
import com.team.hotelbooking.identity.api.dto.response.VerifyEmailResponse;
import com.team.hotelbooking.identity.application.command.LoginCommand;
import com.team.hotelbooking.identity.application.command.RegisterCommand;
import com.team.hotelbooking.identity.application.command.ResetPasswordCommand;
import com.team.hotelbooking.identity.application.result.ForgotPasswordResult;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.result.RegisterResult;
import com.team.hotelbooking.identity.application.result.ResetPasswordResult;
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
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final RegisterUseCase registerUseCase;
  private final VerifyEmailUseCase verifyEmailUseCase;
  private final LoginUseCase loginUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;
  private final LogoutUseCase logoutUseCase;
  private final ForgotPasswordUseCase forgotPasswordUseCase;
  private final ResetPasswordUseCase resetPasswordUseCase;
  private final AuthCookieManager authCookieManager;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<RegisterResponse>> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {

    RegisterResult result =
        registerUseCase.execute(
            new RegisterCommand(
                request.email(), request.password(), request.fullName(), request.phone()));

    RegisterResponse response = RegisterResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Đăng ký thành công.", response));
  }

  @PostMapping("/email-verifications")
  public ResponseEntity<ApiResponse<VerifyEmailResponse>> verifyEmail(
      @Valid @RequestBody VerifyEmailRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {

    VerifyEmailResult result = verifyEmailUseCase.execute(request.token());

    if (result.session() != null) {
      authCookieManager.addAuthCookies(
          servletResponse,
          servletRequest,
          result.session().refreshToken(),
          result.session().rememberMe(),
          result.session().refreshExpiresAt());
    }
    VerifyEmailResponse response = VerifyEmailResponse.from(result);
    var builder = ResponseEntity.ok();
    if (result.session() != null) {
      builder.header("Cache-Control", "no-store").header("Pragma", "no-cache");
    }
    return builder.body(ApiResponse.success("Xác minh email bằng token thành công.", response));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {

    LoginResult result =
        loginUseCase.execute(
            new LoginCommand(
                request.email(), request.password(), Boolean.TRUE.equals(request.rememberMe())));

    authCookieManager.addAuthCookies(
        servletResponse,
        servletRequest,
        result.refreshToken(),
        result.rememberMe(),
        result.refreshExpiresAt());

    LoginResponse response = LoginResponse.from(result, false);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .header("Pragma", "no-cache")
        .body(ApiResponse.success("Đăng nhập thành công.", response));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginResponse>> refresh(
      @Valid @RequestBody(required = false) RefreshTokenRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {

    String rawToken = authCookieManager.extractRefreshToken(servletRequest).orElse(null);
    boolean fromCookie = rawToken != null && !rawToken.isBlank();

    if (fromCookie) {
      authCookieManager.validateCsrf(servletRequest);
    } else if (request != null
        && request.refreshToken() != null
        && !request.refreshToken().isBlank()) {
      rawToken = request.refreshToken();
    } else {
      authCookieManager.clearAuthCookies(servletResponse, servletRequest);
      throw new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
    }

    try {
      LoginResult result = refreshTokenUseCase.execute(rawToken);

      authCookieManager.addAuthCookies(
          servletResponse,
          servletRequest,
          result.refreshToken(),
          result.rememberMe(),
          result.refreshExpiresAt());

      LoginResponse response = LoginResponse.from(result, false);

      return ResponseEntity.ok()
          .header("Cache-Control", "no-store")
          .header("Pragma", "no-cache")
          .body(ApiResponse.success("Làm mới phiên đăng nhập thành công.", response));
    } catch (IdentityException error) {
      authCookieManager.clearAuthCookies(servletResponse, servletRequest);
      throw error;
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @Valid @RequestBody(required = false) LogoutRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {

    String rawToken = authCookieManager.extractRefreshToken(servletRequest).orElse(null);
    boolean fromCookie = rawToken != null && !rawToken.isBlank();

    if (fromCookie) {
      authCookieManager.validateCsrf(servletRequest);
    } else if (request != null
        && request.refreshToken() != null
        && !request.refreshToken().isBlank()) {
      rawToken = request.refreshToken();
    }

    try {
      if (rawToken != null && !rawToken.isBlank()) {
        logoutUseCase.execute(rawToken);
      }
    } finally {
      authCookieManager.clearAuthCookies(servletResponse, servletRequest);
    }

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.<Void>success("Đăng xuất thành công.", null));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {

    ForgotPasswordResult result = forgotPasswordUseCase.execute(request.email());

    ForgotPasswordResponse response = ForgotPasswordResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Nếu tài khoản đủ điều kiện, hệ thống sẽ gửi email đặt lại mật khẩu.", response));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {

    ResetPasswordCommand command = new ResetPasswordCommand(request.token(), request.newPassword());

    ResetPasswordResult result = resetPasswordUseCase.execute(command);

    ResetPasswordResponse response = ResetPasswordResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.", response));
  }
}

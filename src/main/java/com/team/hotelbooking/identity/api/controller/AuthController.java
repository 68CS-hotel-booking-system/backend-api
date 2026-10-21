package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.dto.request.RegisterRequest;
import com.team.hotelbooking.identity.api.dto.request.VerifyEmailRequest;
import com.team.hotelbooking.identity.application.command.RegisterCommand;
import com.team.hotelbooking.identity.application.result.RegisterResult;
import com.team.hotelbooking.identity.application.result.VerifyEmailResult;
import com.team.hotelbooking.identity.application.usecase.RegisterUseCase;
import com.team.hotelbooking.identity.application.usecase.VerifyEmailUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<RegisterResult>> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {

    RegisterResult result =
        registerUseCase.execute(
            new RegisterCommand(
                request.email(), request.password(), request.fullName(), request.phone()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Đăng ký thành công.", result));
  }

  @PostMapping("/email-verifications")
  public ResponseEntity<ApiResponse<VerifyEmailResult>> verifyEmail(
      @Valid @RequestBody VerifyEmailRequest request) {

    VerifyEmailResult result = verifyEmailUseCase.execute(request.token());

    return ResponseEntity.ok(ApiResponse.success("Xác minh email bằng token thành công.", result));
  }
}

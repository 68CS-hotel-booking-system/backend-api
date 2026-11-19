package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.dto.request.ChangePasswordRequest;
import com.team.hotelbooking.identity.api.dto.response.ChangePasswordResponse;
import com.team.hotelbooking.identity.api.dto.response.CurrentUserResponse;
import com.team.hotelbooking.identity.application.command.ChangePasswordCommand;
import com.team.hotelbooking.identity.application.result.ChangePasswordResult;
import com.team.hotelbooking.identity.application.result.CurrentUserResult;
import com.team.hotelbooking.identity.application.usecase.ChangePasswordUseCase;
import com.team.hotelbooking.identity.application.usecase.GetCurrentUserUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final GetCurrentUserUseCase getCurrentUserUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(Principal principal) {

    UUID userId = UUID.fromString(principal.getName());

    CurrentUserResult result = getCurrentUserUseCase.execute(userId);

    CurrentUserResponse response = CurrentUserResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy thông tin tài khoản thành công.", response));
  }

  @PostMapping("/me/password")
  public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request, Principal principal) {

    UUID userId = UUID.fromString(principal.getName());

    ChangePasswordCommand command =
        new ChangePasswordCommand(userId, request.currentPassword(), request.newPassword());

    ChangePasswordResult result = changePasswordUseCase.execute(command);

    ChangePasswordResponse response = ChangePasswordResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.", response));
  }
}

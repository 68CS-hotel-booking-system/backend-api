package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.dto.request.CreateAdminUserRequest;
import com.team.hotelbooking.identity.api.dto.request.ListAdminUsersRequest;
import com.team.hotelbooking.identity.api.dto.request.UpdateUserStatusRequest;
import com.team.hotelbooking.identity.api.dto.response.AdminUserResponse;
import com.team.hotelbooking.identity.api.dto.response.ResendUserVerificationEmailResponse;
import com.team.hotelbooking.identity.application.command.CreateAdminUserCommand;
import com.team.hotelbooking.identity.application.command.ListAdminUsersQuery;
import com.team.hotelbooking.identity.application.command.ResendUserVerificationEmailCommand;
import com.team.hotelbooking.identity.application.command.UpdateUserStatusCommand;
import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.identity.application.result.ResendUserVerificationEmailResult;
import com.team.hotelbooking.identity.application.usecase.CreateAdminUserUseCase;
import com.team.hotelbooking.identity.application.usecase.ListAdminUsersUseCase;
import com.team.hotelbooking.identity.application.usecase.ResendUserVerificationEmailUseCase;
import com.team.hotelbooking.identity.application.usecase.UpdateUserStatusUseCase;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserManagementController {

  private final ListAdminUsersUseCase listAdminUsersUseCase;
  private final CreateAdminUserUseCase createAdminUserUseCase;
  private final UpdateUserStatusUseCase updateUserStatusUseCase;
  private final ResendUserVerificationEmailUseCase resendUserVerificationEmailUseCase;
  private final PaginationSupport pagination;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
      @Valid @ModelAttribute ListAdminUsersRequest request, Principal principal) {
    ListAdminUsersQuery query =
        new ListAdminUsersQuery(
            UUID.fromString(principal.getName()),
            request.search(),
            request.status(),
            request.role(),
            request.createdFrom(),
            request.createdTo(),
            request.sort(),
            pagination.resolve(request.page(), request.size()));

    PageResult<AdminUserResult> result = listAdminUsersUseCase.execute(query);

    PageResponse<AdminUserResponse> response = AdminUserResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách người dùng thành công.", response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
      @Valid @RequestBody CreateAdminUserRequest request, Principal principal) {
    CreateAdminUserCommand command =
        new CreateAdminUserCommand(
            UUID.fromString(principal.getName()),
            request.fullName(),
            request.email(),
            request.phone(),
            request.roles());

    AdminUserResult result = createAdminUserUseCase.execute(command);

    AdminUserResponse response = AdminUserResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Tạo tài khoản thành công.", response));
  }

  @PutMapping("/{userId}/status")
  public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody UpdateUserStatusRequest request,
      Principal principal) {
    UpdateUserStatusCommand command =
        new UpdateUserStatusCommand(
            UUID.fromString(principal.getName()),
            userId,
            request.status(),
            request.reason(),
            request.expectedVersion());

    AdminUserResult result = updateUserStatusUseCase.execute(command);

    AdminUserResponse response = AdminUserResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Cập nhật trạng thái tài khoản thành công.", response));
  }

  @PostMapping("/{userId}/email-verification-requests")
  public ResponseEntity<ApiResponse<ResendUserVerificationEmailResponse>> resendVerificationEmail(
      @PathVariable("userId") UUID userId, Principal principal) {
    ResendUserVerificationEmailCommand command =
        new ResendUserVerificationEmailCommand(UUID.fromString(principal.getName()), userId);

    ResendUserVerificationEmailResult result = resendUserVerificationEmailUseCase.execute(command);

    ResendUserVerificationEmailResponse response = ResendUserVerificationEmailResponse.from(result);

    return ResponseEntity.accepted()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Đã tiếp nhận yêu cầu gửi lại email xác minh.", response));
  }
}

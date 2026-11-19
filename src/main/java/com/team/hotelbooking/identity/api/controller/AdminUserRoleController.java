package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.dto.response.UserRolesResponse;
import com.team.hotelbooking.identity.application.command.GetUserRolesQuery;
import com.team.hotelbooking.identity.application.command.GrantRoleCommand;
import com.team.hotelbooking.identity.application.command.RevokeRoleCommand;
import com.team.hotelbooking.identity.application.result.UserRolesResult;
import com.team.hotelbooking.identity.application.usecase.GetUserRolesUseCase;
import com.team.hotelbooking.identity.application.usecase.GrantRoleUseCase;
import com.team.hotelbooking.identity.application.usecase.RevokeRoleUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserRoleController {

  private final GrantRoleUseCase grantRoleUseCase;
  private final GetUserRolesUseCase getUserRolesUseCase;
  private final RevokeRoleUseCase revokeRoleUseCase;

  @GetMapping("/{userId}/roles")
  public ResponseEntity<ApiResponse<UserRolesResponse>> getUserRoles(
      @PathVariable("userId") UUID userId, Principal principal) {
    GetUserRolesQuery query = new GetUserRolesQuery(UUID.fromString(principal.getName()), userId);

    UserRolesResult result = getUserRolesUseCase.execute(query);

    UserRolesResponse response = UserRolesResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy vai trò của tài khoản thành công.", response));
  }

  @PutMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<ApiResponse<UserRolesResponse>> grantRole(
      @PathVariable("userId") UUID userId,
      @PathVariable("roleId") @Positive(message = "ID vai trò phải lớn hơn 0.") Integer roleId,
      Principal principal) {
    GrantRoleCommand command =
        new GrantRoleCommand(UUID.fromString(principal.getName()), userId, roleId);

    UserRolesResult result = grantRoleUseCase.execute(command);

    UserRolesResponse response = UserRolesResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Cấp quyền thành công.", response));
  }

  @DeleteMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<ApiResponse<UserRolesResponse>> revokeRole(
      @PathVariable("userId") UUID userId,
      @PathVariable("roleId") @Positive(message = "ID vai trò phải lớn hơn 0.") Integer roleId,
      Principal principal) {
    RevokeRoleCommand command =
        new RevokeRoleCommand(UUID.fromString(principal.getName()), userId, roleId);

    UserRolesResult result = revokeRoleUseCase.execute(command);

    UserRolesResponse response = UserRolesResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Thu hồi quyền thành công.", response));
  }
}

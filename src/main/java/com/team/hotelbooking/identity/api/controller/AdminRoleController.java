package com.team.hotelbooking.identity.api.controller;

import com.team.hotelbooking.identity.api.dto.response.RoleResponse;
import com.team.hotelbooking.identity.application.result.RoleResult;
import com.team.hotelbooking.identity.application.usecase.ListRolesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRoleController {

  private final ListRolesUseCase listRolesUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles(Principal principal) {
    List<RoleResult> result = listRolesUseCase.execute(UUID.fromString(principal.getName()));

    List<RoleResponse> response = RoleResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách vai trò thành công.", response));
  }
}

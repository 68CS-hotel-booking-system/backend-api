package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    List<String> roles,
    String status,
    boolean emailVerified,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public AdminUserResponse {
    roles = List.copyOf(roles);
  }

  public static AdminUserResponse from(AdminUserResult result) {
    return new AdminUserResponse(
        result.id(),
        result.fullName(),
        result.email(),
        result.phone(),
        result.roles(),
        result.status(),
        result.emailVerified(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }

  public static PageResponse<AdminUserResponse> from(PageResult<AdminUserResult> result) {
    return PageResponse.from(result.map(AdminUserResponse::from));
  }
}

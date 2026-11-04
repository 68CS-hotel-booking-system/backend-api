package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.RoleResult;
import java.util.List;

public record RoleResponse(Integer id, String roleKey, String roleName) {

  public static RoleResponse from(RoleResult result) {
    return new RoleResponse(result.id(), result.roleKey(), result.roleName());
  }

  public static List<RoleResponse> from(List<RoleResult> results) {
    return results.stream().map(RoleResponse::from).toList();
  }
}

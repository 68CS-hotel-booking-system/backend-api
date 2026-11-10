package com.team.hotelbooking.shared.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;

// Đọc người gọi từ principal do CurrentUserJwtConverter dựng: name là userId, authority ROLE_* là
// role hiện tại trong DB. Chỉ dùng ở tầng API để đưa actor vào Command/Query.
public final class CurrentActor {

  private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

  private CurrentActor() {}

  public static UUID userId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }

  public static boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
  }
}

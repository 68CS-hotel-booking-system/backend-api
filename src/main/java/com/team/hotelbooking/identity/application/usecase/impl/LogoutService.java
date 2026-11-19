package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.usecase.LogoutUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.util.UUID;

public class LogoutService implements LogoutUseCase {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OpaqueTokens opaqueTokens;

  public LogoutService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      OpaqueTokens opaqueTokens) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.opaqueTokens = opaqueTokens;
  }

  @Override
  public void execute(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank() || rawRefreshToken.length() > 128) {

      throw IdentityException.invalid("Refresh token không hợp lệ.");
    }

    String tokenHash = opaqueTokens.hash(rawRefreshToken);

    UUID userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash).orElse(null);

    if (userId == null) {
      return;
    }

    var user = userRepository.findByIdForUpdate(userId).orElse(null);

    if (user == null) {
      return;
    }

    var token = refreshTokenRepository.findByTokenHashForUpdate(tokenHash).orElse(null);

    if (token == null || !token.userId().equals(user.id())) {
      return;
    }

    refreshTokenRepository.revokeFamily(token.familyId());
  }
}

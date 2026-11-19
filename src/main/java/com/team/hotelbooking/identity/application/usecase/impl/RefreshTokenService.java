package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.AccessTokens;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.usecase.RefreshTokenUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.RefreshToken;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class RefreshTokenService implements RefreshTokenUseCase {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OpaqueTokens opaqueTokens;
  private final AccessTokens accessTokens;
  private final Clock clock;

  public RefreshTokenService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      OpaqueTokens opaqueTokens,
      AccessTokens accessTokens,
      Clock clock) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.opaqueTokens = opaqueTokens;
    this.accessTokens = accessTokens;
    this.clock = clock;
  }

  @Override
  public LoginResult execute(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank() || rawRefreshToken.length() > 128) {
      throw new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
    }

    String tokenHash = opaqueTokens.hash(rawRefreshToken);

    UUID userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash).orElse(null);

    if (userId == null) {
      throw new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
    }

    var user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN));

    var token =
        refreshTokenRepository
            .findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN));

    if (!token.userId().equals(user.id())) {
      throw new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
    }

    if (token.revoked()) {
      refreshTokenRepository.revokeFamily(token.familyId());
      throw new IdentityException(IdentityErrorCode.REFRESH_TOKEN_REUSED);
    }

    Instant now = clock.instant();

    if (!token.expiresAt().isAfter(now)) {
      throw new IdentityException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
    }

    user.ensureCanLogin();

    var access = accessTokens.issue(user, now);
    var nextRefresh = opaqueTokens.generate();

    // Thu hồi refresh token vừa được sử dụng.
    refreshTokenRepository.revokeById(token.id());

    // Lưu hash của refresh token mới, giữ nguyên familyId, expiresAt và rememberMe.
    refreshTokenRepository.add(
        new RefreshToken(
            UUID.randomUUID(),
            user.id(),
            token.familyId(),
            nextRefresh.hash(),
            token.expiresAt(),
            false,
            token.rememberMe()));

    // Trả giá trị token mới cho client kèm thông tin phiên.
    return LoginResult.from(
        user,
        access.value(),
        nextRefresh.raw(),
        access.expiresAt(),
        token.rememberMe(),
        token.expiresAt());
  }
}

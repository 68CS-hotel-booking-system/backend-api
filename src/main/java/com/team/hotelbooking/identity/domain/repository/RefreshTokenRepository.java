package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.RefreshToken;

public interface RefreshTokenRepository {

  void add(RefreshToken token);
}

package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.VerificationToken;

public interface VerificationTokenRepository {
  void add(VerificationToken token);
}

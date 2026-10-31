package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.VerificationToken;
import java.util.Optional;

public interface VerificationTokenRepository {
  void add(VerificationToken token);

  Optional<VerificationToken> findByHashForUpdate(String tokenHash);

  void markUsed(VerificationToken token);
}

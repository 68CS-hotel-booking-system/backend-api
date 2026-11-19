package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.repository.GoogleIdentityRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcGoogleIdentityRepository implements GoogleIdentityRepository {
  private final JdbcClient jdbc;

  @Override
  public Optional<UUID> findUserId(String subject) {
    return jdbc.sql("SELECT user_id FROM google_identities WHERE subject = :subject")
        .param("subject", subject)
        .query(UUID.class)
        .optional();
  }

  @Override
  public boolean add(String subject, UUID userId) {
    return jdbc.sql(
                """
        INSERT INTO google_identities (subject, user_id) VALUES (:subject, :userId)
        ON CONFLICT (subject) DO NOTHING
        """)
            .param("subject", subject)
            .param("userId", userId)
            .update()
        == 1;
  }
}

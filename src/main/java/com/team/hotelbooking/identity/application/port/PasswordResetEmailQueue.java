package com.team.hotelbooking.identity.application.port;

import java.time.Instant;
import java.util.UUID;

public interface PasswordResetEmailQueue {

  void enqueue(UUID userId, String recipient, String rawToken, Instant expiresAt);
}

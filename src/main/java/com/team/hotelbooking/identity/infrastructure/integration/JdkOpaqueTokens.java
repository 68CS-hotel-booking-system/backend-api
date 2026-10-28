package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class JdkOpaqueTokens implements OpaqueTokens {

  private final SecureRandom random = new SecureRandom();

  @Override
  public IssuedToken generate() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);

    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    return new IssuedToken(raw, hash(raw));
  }

  @Override
  public String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}

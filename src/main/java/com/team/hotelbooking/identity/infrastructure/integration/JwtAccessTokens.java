package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.AccessTokens;
import com.team.hotelbooking.identity.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokens implements AccessTokens {

  private final JwtEncoder encoder;
  private final String issuer;
  private final String audience;
  private final Duration ttl;

  public JwtAccessTokens(
      JwtEncoder encoder,
      @Value("${auth.jwt.issuer}") String issuer,
      @Value("${auth.jwt.audience}") String audience,
      @Value("${auth.jwt.access-ttl}") String ttl) {

    this.encoder = encoder;
    this.issuer = issuer;
    this.audience = audience;
    this.ttl = Duration.parse(ttl);

    if (this.ttl.isNegative() || this.ttl.isZero()) {
      throw new IllegalArgumentException("Thời gian sống của access token phải lớn hơn 0.");
    }
  }

  @Override
  public IssuedToken issue(User user, Instant now) {
    now = now.truncatedTo(ChronoUnit.SECONDS);

    Instant expiry = now.plus(ttl);

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(user.id().toString())
            .issuedAt(now)
            .expiresAt(expiry)
            .id(UUID.randomUUID().toString())
            .claim("token_use", "access")
            .claim("roles", user.roles().stream().sorted().toList())
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return new IssuedToken(value, expiry);
  }
}

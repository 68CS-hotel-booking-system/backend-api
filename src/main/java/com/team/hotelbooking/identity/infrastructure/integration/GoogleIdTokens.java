package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.usecase.impl.GoogleAccountService.VerifiedAccount;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleIdTokens {
  private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
  private static final Set<String> ISSUERS =
      Set.of("https://accounts.google.com", "accounts.google.com");
  private final String clientId;
  private final JwtDecoder decoder;

  @Autowired
  public GoogleIdTokens(@Value("${auth.google.client-id:}") String clientId) {
    this(clientId.strip(), clientId.isBlank() ? null : createDecoder());
  }

  GoogleIdTokens(String clientId, JwtDecoder decoder) {
    this.clientId = clientId;
    this.decoder = decoder;
  }

  public String clientId() {
    return clientId;
  }

  public VerifiedAccount verify(String credential, String nonce) {
    if (decoder == null) {
      throw new IdentityException(IdentityErrorCode.GOOGLE_UNAVAILABLE);
    }
    if (nonce == null || nonce.isBlank()) {
      throw invalid();
    }
    Jwt jwt;
    try {
      jwt = decoder.decode(credential);
    } catch (JwtException | IllegalArgumentException invalidToken) {
      throw invalid();
    }
    String issuer = jwt.getClaimAsString("iss");
    String subject = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    if (issuer == null
        || !ISSUERS.contains(issuer)
        || !List.of(clientId).equals(jwt.getAudience())
        || jwt.getExpiresAt() == null
        || !nonce.equals(jwt.getClaimAsString("nonce"))
        || subject == null
        || subject.isBlank()
        || subject.length() > 255
        || email == null
        || email.isBlank()
        || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
      throw invalid();
    }
    String hostedDomain = jwt.getClaimAsString("hd");
    boolean authoritative =
        email.toLowerCase(Locale.ROOT).endsWith("@gmail.com")
            || (hostedDomain != null && !hostedDomain.isBlank());
    return new VerifiedAccount(subject, email, jwt.getClaimAsString("name"), authoritative);
  }

  private static IdentityException invalid() {
    return new IdentityException(IdentityErrorCode.GOOGLE_TOKEN_INVALID);
  }

  private static JwtDecoder createDecoder() {
    var transport = new SimpleClientHttpRequestFactory();
    transport.setConnectTimeout(Duration.ofSeconds(3));
    transport.setReadTimeout(Duration.ofSeconds(3));
    return NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS)
        .restOperations(new RestTemplate(transport))
        .build();
  }
}

package com.team.hotelbooking.identity.infrastructure.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class GoogleIdTokensTests {
  private static final String CLIENT_ID = "stayhub-test.apps.googleusercontent.com";
  private static final String ISSUER = "https://accounts.google.com";
  private static final String NONCE = "browser-nonce";
  private static RSAKey key;
  private static GoogleIdTokens tokens;

  @BeforeAll
  static void setup() throws JOSEException {
    key = new RSAKeyGenerator(2048).generate();
    tokens =
        new GoogleIdTokens(CLIENT_ID, NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build());
  }

  @Test
  void acceptsOnlyVerifiedIdentitiesAndIdentifiesAuthoritativeAddresses() throws JOSEException {
    var gmail = tokens.verify(signed(claims().build(), key), NONCE);
    assertThat(gmail.subject()).isEqualTo("google-subject");
    assertThat(gmail.authoritativeEmail()).isTrue();
    var external = claims().claim("email", "guest@example.com");
    assertThat(tokens.verify(signed(external.build(), key), NONCE).authoritativeEmail()).isFalse();
    assertThat(
            tokens
                .verify(signed(external.claim("hd", "example.com").build(), key), NONCE)
                .authoritativeEmail())
        .isTrue();
  }

  @Test
  void rejectsWrongSignatureAudienceIssuerNonceAndExpiredOrUnverifiedTokens() throws JOSEException {
    var unrelatedKey = new RSAKeyGenerator(2048).generate();
    assertInvalid(signed(claims().build(), unrelatedKey), NONCE);
    assertInvalid(signed(claims().audience("another-client").build(), key), NONCE);
    assertInvalid(signed(claims().issuer("https://example.com").build(), key), NONCE);
    assertInvalid(
        signed(claims().expirationTime(Date.from(Instant.now().minusSeconds(120))).build(), key),
        NONCE);
    assertInvalid(signed(claims().claim("email_verified", false).build(), key), NONCE);
    assertInvalid(signed(claims().build(), key), "another-browser");
    assertInvalid(signed(claims().build(), key), null);
  }

  private static JWTClaimsSet.Builder claims() {
    return new JWTClaimsSet.Builder()
        .issuer(ISSUER)
        .audience(CLIENT_ID)
        .subject("google-subject")
        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
        .claim("email", "guest@gmail.com")
        .claim("email_verified", true)
        .claim("nonce", NONCE);
  }

  private static String signed(JWTClaimsSet claims, RSAKey signer) throws JOSEException {
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    jwt.sign(new RSASSASigner(signer));
    return jwt.serialize();
  }

  private static void assertInvalid(String credential, String nonce) {
    assertThatThrownBy(() -> tokens.verify(credential, nonce))
        .isInstanceOfSatisfying(
            IdentityException.class,
            error -> assertThat(error.code()).isEqualTo(IdentityErrorCode.GOOGLE_TOKEN_INVALID));
  }
}

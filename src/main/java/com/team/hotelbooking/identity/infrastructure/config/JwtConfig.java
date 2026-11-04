package com.team.hotelbooking.identity.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

  @Bean
  public SecretKey jwtSecretKey(@Value("${auth.jwt.secret-base64}") String encoded) {
    byte[] bytes = Base64.getDecoder().decode(encoded);

    if (bytes.length < 32) {
      throw new IllegalArgumentException("Khóa JWT phải chứa ít nhất 32 byte ngẫu nhiên.");
    }

    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
  }

  @Bean
  public JwtDecoder jwtDecoder(
      SecretKey jwtSecretKey,
      @Value("${auth.jwt.issuer}") String issuer,
      @Value("${auth.jwt.audience}") String audience) {

    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();

    var audienceValidator =
        new JwtClaimValidator<List<String>>(
            "aud", value -> value != null && value.contains(audience));

    var purposeValidator = new JwtClaimValidator<String>("token_use", "access"::equals);

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, purposeValidator));

    return decoder;
  }
}

package com.team.hotelbooking.identity.infrastructure.security;

import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserRepository users;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UUID userId;

    try {
      userId = UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new InvalidBearerTokenException("Invalid token subject.");
    }

    User user =
        users
            .findById(userId)
            .orElseThrow(() -> new InvalidBearerTokenException("Account unavailable."));

    if (user.status() == User.Status.BANNED || user.status() == User.Status.INACTIVE) {

      throw new InvalidBearerTokenException("Account unavailable.");
    }

    var authorities =
        user.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();

    return new JwtAuthenticationToken(jwt, authorities, userId.toString());
  }
}

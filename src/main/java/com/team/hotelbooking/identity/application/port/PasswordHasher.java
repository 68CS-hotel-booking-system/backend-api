package com.team.hotelbooking.identity.application.port;

public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String encodedPassword);
}

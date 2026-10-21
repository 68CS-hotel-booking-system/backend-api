package com.team.hotelbooking.identity.domain.service;

import com.team.hotelbooking.identity.domain.exception.IdentityException;

public final class PasswordPolicy {

  private PasswordPolicy() {}

  public static void validateNewPassword(String password) {
    if (password == null
        || password.length() < 15
        || password.length() > 128
        || password.isBlank()) {

      throw IdentityException.invalid("Mật khẩu phải có từ 15 đến 128 ký tự.");
    }
  }
}

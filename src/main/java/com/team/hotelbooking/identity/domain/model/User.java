package com.team.hotelbooking.identity.domain.model;

import com.team.hotelbooking.identity.domain.exception.IdentityException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record User(
    UUID id,
    String email,
    String passwordHash,
    String fullName,
    String phone,
    Status status,
    Instant emailVerifiedAt,
    Set<String> roles,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public enum Status {
    ACTIVE,
    INACTIVE,
    PENDING,
    BANNED
  }

  public User {
    roles = Set.copyOf(roles);
  }

  public static User register(
      UUID id, String email, String passwordHash, String fullName, String phone, Instant now) {

    String name = fullName == null ? "" : fullName.trim();

    if (name.isBlank() || name.length() > 100) {
      throw IdentityException.invalid("Họ tên phải có từ 1 đến 100 ký tự.");
    }

    return new User(
        id,
        normalizeEmail(email),
        passwordHash,
        name,
        normalizePhone(phone),
        Status.PENDING,
        null,
        Set.of("CUSTOMER"),
        now,
        now,
        0L);
  }

  public static String normalizeEmail(String raw) {
    if (raw == null) {
      throw IdentityException.invalid("Email không được để trống.");
    }

    String email = raw.trim().toLowerCase(Locale.ROOT);
    int at = email.indexOf('@');

    if (email.length() > 255
        || at <= 0
        || at == email.length() - 1
        || email.chars().anyMatch(Character::isWhitespace)) {

      throw IdentityException.invalid("Email không hợp lệ.");
    }

    return email;
  }

  public static String normalizePhone(String raw) {
    if (raw == null) {
      throw IdentityException.invalid("Số điện thoại không được để trống.");
    }

    String phone = raw.replaceAll("[\\s().-]", "");

    if (phone.startsWith("00")) {
      phone = "+" + phone.substring(2);
    } else if (phone.startsWith("0")) {
      if (!phone.matches("0[0-9]{9,10}")) {
        throw IdentityException.invalid("Số điện thoại Việt Nam không hợp lệ.");
      }
      phone = "+84" + phone.substring(1);
    }

    if (!phone.matches("\\+[1-9][0-9]{7,14}")) {
      throw IdentityException.invalid("Số điện thoại phải có mã quốc gia hợp lệ.");
    }

    return phone;
  }

  @Override
  public String toString() {
    return "User[id=" + id + ", status=" + status + "]";
  }
}

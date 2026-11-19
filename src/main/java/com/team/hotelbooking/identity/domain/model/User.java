package com.team.hotelbooking.identity.domain.model;

import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
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
    long authVersion,
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
        0L, // authVersion
        0L); // version
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

  public User verifyEmail(Instant now) {
    if (status != Status.PENDING || emailVerifiedAt != null) {
      throw new IdentityException(IdentityErrorCode.CONFLICT);
    }
    return new User(
        id,
        email,
        passwordHash,
        fullName,
        phone,
        Status.ACTIVE,
        now,
        roles,
        createdAt,
        now,
        authVersion,
        version);
  }

  public void ensureCanLogin() {
    if (status == Status.BANNED || status == Status.INACTIVE) {
      throw new IdentityException(IdentityErrorCode.ACCOUNT_LOCKED);
    }

    if (status == Status.PENDING || emailVerifiedAt == null) {
      throw new IdentityException(IdentityErrorCode.EMAIL_NOT_VERIFIED);
    }
  }

  public User resetPassword(String newPasswordHash, Instant now) {
    if (status == Status.BANNED || status == Status.INACTIVE) {
      throw new IdentityException(IdentityErrorCode.ACCOUNT_LOCKED);
    }

    return new User(
        id,
        email,
        newPasswordHash,
        fullName,
        phone,
        status,
        emailVerifiedAt,
        roles,
        createdAt,
        now,
        authVersion + 1,
        version);
  }

  public User changeStatus(Status newStatus, Instant now) {
    if (newStatus == null) {
      throw IdentityException.invalid("Trạng thái đích là bắt buộc.");
    }

    if (newStatus == Status.ACTIVE && emailVerifiedAt == null) {
      throw new IdentityException(
          IdentityErrorCode.INVALID_USER_STATUS_TRANSITION,
          "Tài khoản chưa xác minh email chỉ có thể mở khóa về pending.");
    }

    if (newStatus == Status.PENDING && emailVerifiedAt != null) {
      throw new IdentityException(
          IdentityErrorCode.INVALID_USER_STATUS_TRANSITION,
          "Không thể chuyển tài khoản đã xác minh email về pending.");
    }

    if (newStatus == status) {
      return this;
    }

    return new User(
        id,
        email,
        passwordHash,
        fullName,
        phone,
        newStatus,
        emailVerifiedAt,
        roles,
        createdAt,
        now,
        authVersion + 1,
        version);
  }

  @Override
  public String toString() {
    return "User[id=" + id + ", status=" + status + "]";
  }
}

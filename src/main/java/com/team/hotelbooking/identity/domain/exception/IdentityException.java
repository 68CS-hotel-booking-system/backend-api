package com.team.hotelbooking.identity.domain.exception;

public final class IdentityException extends RuntimeException {

  private final IdentityErrorCode code;

  public IdentityException(IdentityErrorCode code) {
    this(code, code.defaultMessage());
  }

  public IdentityException(IdentityErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public IdentityErrorCode code() {
    return code;
  }

  public static IdentityException invalid(String message) {
    return new IdentityException(IdentityErrorCode.VALIDATION_FAILED, message);
  }
}

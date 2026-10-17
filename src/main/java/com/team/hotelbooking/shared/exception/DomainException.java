package com.team.hotelbooking.shared.exception;

import java.util.Objects;

/** Base class for domain failures. HTTP mapping belongs to the web layer. */
public abstract class DomainException extends RuntimeException {

  private final ErrorCode errorCode;

  protected DomainException(ErrorCode errorCode) {
    this(
        errorCode,
        Objects.requireNonNull(errorCode, "errorCode must not be null").defaultMessage());
  }

  /** The message is exposed to API clients and must not contain internal details. */
  protected DomainException(ErrorCode errorCode, String message) {
    super(Objects.requireNonNull(message, "message must not be null"));
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}

package com.team.hotelbooking.shared.exception;

public class ConflictException extends DomainException {

  public ConflictException(ErrorCode errorCode) {
    super(errorCode);
  }

  public ConflictException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}

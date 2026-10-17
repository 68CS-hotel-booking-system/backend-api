package com.team.hotelbooking.shared.exception;

public class BusinessRuleException extends DomainException {

  public BusinessRuleException(ErrorCode errorCode) {
    super(errorCode);
  }

  public BusinessRuleException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}

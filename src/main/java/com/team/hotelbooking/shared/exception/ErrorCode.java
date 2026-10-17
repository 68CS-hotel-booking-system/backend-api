package com.team.hotelbooking.shared.exception;

/** Implemented by each module's error codes; contains no transport-specific details. */
public interface ErrorCode {

  String code();

  String defaultMessage();
}

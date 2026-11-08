package com.team.hotelbooking.media.domain.exception;

public final class MediaException extends RuntimeException {

  private final MediaErrorCode code;

  public MediaException(MediaErrorCode code) {
    this(code, code.defaultMessage());
  }

  public MediaException(MediaErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public MediaErrorCode code() {
    return code;
  }
}

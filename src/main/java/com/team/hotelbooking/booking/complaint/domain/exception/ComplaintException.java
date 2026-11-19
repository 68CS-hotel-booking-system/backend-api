package com.team.hotelbooking.booking.complaint.domain.exception;

import java.util.UUID;

public final class ComplaintException extends RuntimeException {

  private final ComplaintErrorCode code;
  // Chỉ có giá trị với COMPLAINT_ALREADY_OPEN: khiếu nại đang mở để FE điều hướng.
  private final UUID openComplaintId;

  public ComplaintException(ComplaintErrorCode code) {
    this(code, code.defaultMessage());
  }

  public ComplaintException(ComplaintErrorCode code, String message) {
    super(message);
    this.code = code;
    this.openComplaintId = null;
  }

  private ComplaintException(ComplaintErrorCode code, UUID openComplaintId) {
    super(code.defaultMessage());
    this.code = code;
    this.openComplaintId = openComplaintId;
  }

  public static ComplaintException alreadyOpen(UUID openComplaintId) {
    return new ComplaintException(ComplaintErrorCode.COMPLAINT_ALREADY_OPEN, openComplaintId);
  }

  public static ComplaintException invalid(String message) {
    return new ComplaintException(ComplaintErrorCode.VALIDATION_FAILED, message);
  }

  public ComplaintErrorCode code() {
    return code;
  }

  public UUID openComplaintId() {
    return openComplaintId;
  }
}

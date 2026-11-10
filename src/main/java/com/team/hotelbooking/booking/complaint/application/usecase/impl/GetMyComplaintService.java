package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.complaint.application.command.GetMyComplaintQuery;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.GetMyComplaintUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintErrorCode;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintRepository;

// Khiếu nại của người khác trả 404 như không tồn tại, không lộ là có.
public final class GetMyComplaintService implements GetMyComplaintUseCase {

  private final ComplaintRepository complaints;

  public GetMyComplaintService(ComplaintRepository complaints) {
    this.complaints = complaints;
  }

  @Override
  public ComplaintResult execute(GetMyComplaintQuery query) {
    if (query == null || query.customerId() == null || query.complaintId() == null) {
      throw ComplaintException.invalid("Thiếu người gọi hoặc mã khiếu nại.");
    }
    return complaints
        .findByIdAndCustomerId(query.complaintId(), query.customerId())
        .map(ComplaintResult::from)
        .orElseThrow(() -> new ComplaintException(ComplaintErrorCode.COMPLAINT_NOT_FOUND));
  }
}

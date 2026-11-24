package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.complaint.application.command.GetManagedComplaintQuery;
import com.team.hotelbooking.booking.complaint.application.port.ComplaintHotelPort;
import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.GetManagedComplaintUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.*;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintCaseRepository;

public final class GetManagedComplaintService implements GetManagedComplaintUseCase {
  private final ComplaintCaseRepository cases;
  private final ComplaintManagementAccess access;

  public GetManagedComplaintService(
      ComplaintCaseRepository cases, BookingIdentityPort accounts, ComplaintHotelPort hotels) {
    this.cases = cases;
    this.access = new ComplaintManagementAccess(accounts, hotels);
  }

  public ManagedComplaintResult execute(GetManagedComplaintQuery query) {
    if (query == null || query.actorId() == null || query.complaintId() == null)
      throw ComplaintException.invalid("Thiếu người gọi hoặc mã khiếu nại.");
    var scope = access.require(query.actorId());
    var value =
        cases
            .findCase(query.complaintId())
            .orElseThrow(() -> new ComplaintException(ComplaintErrorCode.COMPLAINT_NOT_FOUND));
    scope.requireHotel(value.complaint().hotelId());
    return ManagedComplaintResult.from(
        value, scope.hotels().get(value.complaint().hotelId()), cases.history(query.complaintId()));
  }
}

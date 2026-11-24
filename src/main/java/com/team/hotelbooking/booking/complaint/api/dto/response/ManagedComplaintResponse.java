package com.team.hotelbooking.booking.complaint.api.dto.response;

import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;

public record ManagedComplaintResponse(
    ComplaintResponse complaint,
    String hotelName,
    String bookingCode,
    String bookerName,
    String bookerEmail,
    String bookerPhone) {
  public static ManagedComplaintResponse from(ManagedComplaintResult value) {
    return new ManagedComplaintResponse(
        ComplaintResponse.from(value.complaint()),
        value.hotelName(),
        value.bookingCode(),
        value.bookerName(),
        value.bookerEmail(),
        value.bookerPhone());
  }

  public static PageResponse<ManagedComplaintResponse> from(
      PageResult<ManagedComplaintResult> value) {
    return new PageResponse<>(
        value.content().stream().map(ManagedComplaintResponse::from).toList(),
        value.page(),
        value.size(),
        value.totalElements(),
        value.totalPages(),
        value.first(),
        value.last());
  }
}

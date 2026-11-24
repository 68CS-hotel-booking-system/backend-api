package com.team.hotelbooking.booking.complaint.application.result;

import com.team.hotelbooking.booking.complaint.domain.model.*;
import java.util.List;

public record ManagedComplaintResult(
    ComplaintResult complaint,
    String hotelName,
    String bookingCode,
    String bookerName,
    String bookerEmail,
    String bookerPhone) {
  public static ManagedComplaintResult from(
      ComplaintCase value, String hotelName, List<ComplaintAction> history) {
    return new ManagedComplaintResult(
        ComplaintResult.from(value.complaint(), history),
        hotelName,
        value.bookingCode(),
        value.bookerName(),
        value.bookerEmail(),
        value.bookerPhone());
  }
}

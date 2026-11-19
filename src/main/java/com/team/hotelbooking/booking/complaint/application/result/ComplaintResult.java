package com.team.hotelbooking.booking.complaint.application.result;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import java.time.Instant;
import java.util.UUID;

public record ComplaintResult(
    UUID id,
    UUID bookingId,
    UUID hotelId,
    String category,
    String content,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  public static ComplaintResult from(Complaint complaint) {
    return new ComplaintResult(
        complaint.id(),
        complaint.bookingId(),
        complaint.hotelId(),
        complaint.category().name(),
        complaint.content(),
        complaint.status().name(),
        complaint.createdAt(),
        complaint.updatedAt());
  }
}

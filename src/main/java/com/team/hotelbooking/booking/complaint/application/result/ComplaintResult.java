package com.team.hotelbooking.booking.complaint.application.result;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.booking.complaint.domain.model.ComplaintAction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ComplaintResult(
    UUID id,
    UUID bookingId,
    UUID hotelId,
    String category,
    String content,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version,
    List<ComplaintActionResult> history) {

  public ComplaintResult {
    history = List.copyOf(history);
  }

  public static ComplaintResult from(Complaint complaint) {
    return from(complaint, List.of());
  }

  public static ComplaintResult from(Complaint complaint, List<ComplaintAction> history) {
    return new ComplaintResult(
        complaint.id(),
        complaint.bookingId(),
        complaint.hotelId(),
        complaint.category().name(),
        complaint.content(),
        complaint.status().name(),
        complaint.createdAt(),
        complaint.updatedAt(),
        complaint.version(),
        history.stream().map(ComplaintActionResult::from).toList());
  }
}

package com.team.hotelbooking.booking.complaint.application.result;

import com.team.hotelbooking.booking.complaint.domain.model.ComplaintAction;
import java.time.Instant;
import java.util.UUID;

public record ComplaintActionResult(
    UUID id,
    String actorRole,
    String fromStatus,
    String toStatus,
    String response,
    Instant createdAt,
    long version) {
  public static ComplaintActionResult from(ComplaintAction value) {
    return new ComplaintActionResult(
        value.id(),
        value.actorRole(),
        value.fromStatus().name(),
        value.toStatus().name(),
        value.response(),
        value.createdAt(),
        value.version());
  }
}

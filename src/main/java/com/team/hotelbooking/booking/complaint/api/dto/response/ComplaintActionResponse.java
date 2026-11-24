package com.team.hotelbooking.booking.complaint.api.dto.response;

import com.team.hotelbooking.booking.complaint.application.result.ComplaintActionResult;
import java.time.Instant;
import java.util.UUID;

public record ComplaintActionResponse(
    UUID id,
    String actorRole,
    String fromStatus,
    String toStatus,
    String response,
    Instant createdAt,
    long version) {
  public static ComplaintActionResponse from(ComplaintActionResult value) {
    return new ComplaintActionResponse(
        value.id(),
        value.actorRole(),
        value.fromStatus(),
        value.toStatus(),
        value.response(),
        value.createdAt(),
        value.version());
  }
}

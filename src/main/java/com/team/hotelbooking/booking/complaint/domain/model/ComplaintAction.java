package com.team.hotelbooking.booking.complaint.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ComplaintAction(
    UUID id,
    UUID complaintId,
    UUID actorUserId,
    String actorRole,
    Complaint.Status fromStatus,
    Complaint.Status toStatus,
    String response,
    Instant createdAt,
    long version) {}

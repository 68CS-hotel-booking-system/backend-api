package com.team.hotelbooking.booking.complaint.application.command;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import java.util.UUID;

public record HandleComplaintCommand(
    UUID actorId,
    UUID complaintId,
    Complaint.Status status,
    String response,
    Long expectedVersion) {}

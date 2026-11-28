package com.team.hotelbooking.booking.complaint.application.command;

import java.util.UUID;

public record GetManagedComplaintQuery(UUID actorId, UUID complaintId) {}

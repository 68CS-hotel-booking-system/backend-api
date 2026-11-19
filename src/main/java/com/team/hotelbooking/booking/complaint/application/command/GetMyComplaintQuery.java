package com.team.hotelbooking.booking.complaint.application.command;

import java.util.UUID;

public record GetMyComplaintQuery(UUID customerId, UUID complaintId) {}

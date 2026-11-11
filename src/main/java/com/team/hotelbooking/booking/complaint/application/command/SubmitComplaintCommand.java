package com.team.hotelbooking.booking.complaint.application.command;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import java.util.UUID;

// customerId lấy từ principal; không nhận từ body.
public record SubmitComplaintCommand(
    UUID customerId, UUID bookingId, Complaint.Category category, String content) {}

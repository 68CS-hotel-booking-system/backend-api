package com.team.hotelbooking.booking.complaint.application.command;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListManagedComplaintsQuery(
    UUID actorId, UUID hotelId, Complaint.Status status, String keyword, PageQuery pagination) {}

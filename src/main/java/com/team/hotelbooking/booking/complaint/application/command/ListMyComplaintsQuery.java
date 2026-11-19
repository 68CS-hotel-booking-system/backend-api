package com.team.hotelbooking.booking.complaint.application.command;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

// status null = mọi trạng thái.
public record ListMyComplaintsQuery(
    UUID customerId, Complaint.Status status, PageQuery pagination) {}

package com.team.hotelbooking.payment.application.command;

import com.team.hotelbooking.payment.domain.model.Refund;
import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListRefundsQuery(
    UUID actorId,
    boolean management,
    Refund.Status status,
    UUID bookingId,
    String keyword,
    PageQuery pagination) {}

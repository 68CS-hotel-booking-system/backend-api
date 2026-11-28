package com.team.hotelbooking.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefundAction(
    UUID id,
    UUID refundId,
    UUID actorId,
    Refund.Status fromStatus,
    Refund.Status toStatus,
    String note,
    String settlementReference,
    Instant createdAt,
    long version) {}

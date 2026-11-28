package com.team.hotelbooking.payment.application.command;

import com.team.hotelbooking.payment.domain.model.Refund;
import java.util.UUID;

public record ProcessRefundCommand(
    UUID actorId,
    UUID refundId,
    Refund.Status status,
    String reference,
    String note,
    Long expectedVersion) {}

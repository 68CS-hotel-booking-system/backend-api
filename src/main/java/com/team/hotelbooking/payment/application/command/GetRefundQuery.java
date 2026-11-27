package com.team.hotelbooking.payment.application.command;

import java.util.UUID;

public record GetRefundQuery(UUID actorId, boolean management, UUID refundId) {}

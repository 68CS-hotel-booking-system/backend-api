package com.team.hotelbooking.payment.application.command;

import java.util.UUID;

public record UpdateFinancialAccessCommand(
    UUID hotelId,
    UUID targetUserId,
    boolean isGranted,
    String accessScope,
    String reason,
    Long expectedVersion,
    UUID currentAdminUserId) {}

package com.team.hotelbooking.payment.application.command;

import java.util.UUID;

public record GetFinancialAccessQuery(UUID hotelId, UUID targetUserId) {}

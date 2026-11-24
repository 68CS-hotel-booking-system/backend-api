package com.team.hotelbooking.payment.application.command;

import java.util.UUID;

public record GetFinanceDashboardQuery(UUID actorId, boolean admin) {}

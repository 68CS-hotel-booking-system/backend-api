package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record RejectHotelReviewCommand(
    UUID actorId, UUID requestId, Long expectedVersion, String reason) {}

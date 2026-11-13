package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record ApproveHotelReviewCommand(UUID actorId, UUID requestId, Long expectedVersion) {}

package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record GetHotelReviewQuery(UUID actorId, UUID requestId) {}

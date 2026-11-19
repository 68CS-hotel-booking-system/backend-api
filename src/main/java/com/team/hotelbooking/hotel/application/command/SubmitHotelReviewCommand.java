package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record SubmitHotelReviewCommand(UUID actorId, UUID hotelId, Long expectedVersion) {}

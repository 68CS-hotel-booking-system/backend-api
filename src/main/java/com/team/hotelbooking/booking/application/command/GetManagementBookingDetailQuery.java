package com.team.hotelbooking.booking.application.command;

import java.util.UUID;

public record GetManagementBookingDetailQuery(UUID actorId, UUID hotelId, UUID bookingId) {}

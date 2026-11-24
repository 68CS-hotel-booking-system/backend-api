package com.team.hotelbooking.booking.application.command;

import java.util.UUID;

public record CancelBookingCommand(UUID customerId, UUID bookingId) {}

package com.team.hotelbooking.booking.application.command;

import java.util.UUID;

public record GetCustomerBookingDetailQuery(UUID customerId, UUID bookingId) {}

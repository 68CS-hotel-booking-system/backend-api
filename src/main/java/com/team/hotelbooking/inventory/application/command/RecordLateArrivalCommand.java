package com.team.hotelbooking.inventory.application.command;

import java.time.Instant;
import java.util.UUID;

public record RecordLateArrivalCommand(
    UUID actorId, UUID hotelId, UUID bookingId, Instant expectedArrivalAt, String note) {}

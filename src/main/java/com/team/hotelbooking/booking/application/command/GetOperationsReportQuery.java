package com.team.hotelbooking.booking.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record GetOperationsReportQuery(
    UUID actorId, UUID hotelId, LocalDate from, LocalDate to) {}

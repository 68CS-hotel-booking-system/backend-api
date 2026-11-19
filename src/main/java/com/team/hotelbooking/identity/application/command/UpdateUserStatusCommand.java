package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record UpdateUserStatusCommand(
    UUID actorId, UUID userId, String status, String reason, long expectedVersion) {}

package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record ResendUserVerificationEmailCommand(UUID actorId, UUID userId) {}

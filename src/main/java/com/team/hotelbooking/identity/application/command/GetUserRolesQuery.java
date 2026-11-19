package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record GetUserRolesQuery(UUID actorId, UUID targetUserId) {}

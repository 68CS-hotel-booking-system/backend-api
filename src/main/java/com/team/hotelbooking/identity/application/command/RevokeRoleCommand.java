package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record RevokeRoleCommand(UUID actorId, UUID targetUserId, Integer roleId) {}

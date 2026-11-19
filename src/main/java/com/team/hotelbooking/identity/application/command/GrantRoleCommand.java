package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record GrantRoleCommand(UUID actorId, UUID targetUserId, Integer roleId) {}

// MediaAccessQuery.java
package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record MediaAccessQuery(UUID actorId, UUID mediaId) {}

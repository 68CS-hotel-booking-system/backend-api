// MediaAccessResult.java
package com.team.hotelbooking.media.application.result;

import java.time.Instant;
import java.util.UUID;

public record MediaAccessResult(UUID mediaId, String url, Instant expiresAt) {}

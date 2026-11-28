package com.team.hotelbooking.payment.application.port;

import java.util.*;

public interface RefundAccessPort {
  Optional<Viewer> viewer(UUID actorId);

  Map<UUID, String> hotels(UUID ownerId);

  record Viewer(boolean active, boolean verified, boolean admin, boolean owner) {}
}

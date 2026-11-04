package com.team.hotelbooking.inventory.application.port;

import java.time.LocalDate;
import java.util.UUID;

public interface InventoryQueryPort {
  int calculateAvailableQuantity(UUID roomTypeId, LocalDate checkInDate, LocalDate checkOutDate);
}

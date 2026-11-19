package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.UUID;

public interface RoomTypeDailyOccupancy {
  UUID getRoomTypeId();

  LocalDate getStayDate();

  long getTotalQuantity();
}

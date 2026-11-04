package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import java.time.LocalDate;

public interface DailyReservationOccupancy {
  LocalDate getStayDate();

  long getTotalQuantity();
}

package com.team.hotelbooking;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(HotelBookingBackendApplication.class);

  @Test
  void verifyModularity() {
    modules.verify();
  }

  @Test
  void printModules() {
    modules.forEach(System.out::println);
  }
}

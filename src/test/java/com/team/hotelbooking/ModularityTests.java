package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(HotelBookingBackendApplication.class);

  @Test
  @DisplayName("Module pricing phải bảo vệ các package nội bộ")
  void shouldKeepPricingModuleClosed() {
    assertFalse(modules.getModuleByName("pricing").orElseThrow().isOpen());
  }

  @Test
  void verifyModularity() {
    modules.verify();
  }

  @Test
  void printModules() {
    modules.forEach(System.out::println);
  }
}

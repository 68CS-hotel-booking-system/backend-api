package com.team.hotelbooking;

import org.springframework.boot.SpringApplication;

public class TestHotelBookingApplication {

  public static void main(String[] args) {
    SpringApplication.from(HotelBookingBackendApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}

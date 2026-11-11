package com.team.hotelbooking;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HotelBookingBackendApplication {

  public static void main(String[] args) {
    // Align JVM and JDBC time zones before Hibernate initializes TIME mappings.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(HotelBookingBackendApplication.class, args);
  }
}

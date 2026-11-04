package com.team.hotelbooking.inventory.infrastructure.config;

import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.application.service.CheckAvailabilityService;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class InventoryConfig {

  @Bean
  public CheckAvailabilityUseCase checkAvailabilityUseCase(
      HotelCatalogPort hotelCatalogPort,
      InventoryQueryPort inventoryQueryPort,
      RoomPricingPort roomPricingPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CheckAvailabilityService service =
        new CheckAvailabilityService(hotelCatalogPort, inventoryQueryPort, roomPricingPort, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

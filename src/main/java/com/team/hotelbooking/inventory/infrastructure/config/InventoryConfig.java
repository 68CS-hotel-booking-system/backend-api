package com.team.hotelbooking.inventory.infrastructure.config;

import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCoverImagePort;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.application.port.StayPricingPort;
import com.team.hotelbooking.inventory.application.service.CheckAvailabilityService;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.application.usecase.SearchHotelsUseCase;
import com.team.hotelbooking.inventory.application.usecase.impl.SearchHotelsService;
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

  // Chỉ đọc; tồn, giá và ảnh bìa lấy theo lô qua port, tham gia cùng transaction read-only này.
  @Bean
  public SearchHotelsUseCase searchHotelsUseCase(
      HotelSearchCatalogPort hotelSearchCatalogPort,
      InventoryQueryPort inventoryQueryPort,
      StayPricingPort stayPricingPort,
      HotelCoverImagePort hotelCoverImagePort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    SearchHotelsService service =
        new SearchHotelsService(
            hotelSearchCatalogPort,
            inventoryQueryPort,
            stayPricingPort,
            hotelCoverImagePort,
            clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

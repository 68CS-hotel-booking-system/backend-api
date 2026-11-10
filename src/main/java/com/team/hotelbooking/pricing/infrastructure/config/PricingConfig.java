package com.team.hotelbooking.pricing.infrastructure.config;

import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.service.GetRoomRatesService;
import com.team.hotelbooking.pricing.application.service.SetRoomRatesService;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
import com.team.hotelbooking.pricing.application.usecase.SetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class PricingConfig {

  @Bean
  public GetRoomRatesUseCase getRoomRatesUseCase(
      PricingHotelPort pricingHotelPort,
      RoomRateRepository roomRateRepository,
      PlatformTransactionManager transactionManager) {

    GetRoomRatesService service = new GetRoomRatesService(pricingHotelPort, roomRateRepository);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public SetRoomRatesUseCase setRoomRatesUseCase(
      PricingHotelPort pricingHotelPort,
      RoomRateRepository roomRateRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    SetRoomRatesService service =
        new SetRoomRatesService(pricingHotelPort, roomRateRepository, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }
}

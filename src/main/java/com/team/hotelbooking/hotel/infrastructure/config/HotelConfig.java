package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.GetManagementHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListManagementHotelsService;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class HotelConfig {

  @Bean
  public CreateHotelUseCase createHotelUseCase(
      HotelRepository hotels,
      CityRepository cities,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CreateHotelService service = new CreateHotelService(hotels, cities, accounts, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public GetManagementHotelUseCase getManagementHotelUseCase(
      HotelRepository hotels,
      IdentityAccounts accounts,
      PlatformTransactionManager transactionManager) {
    GetManagementHotelService service = new GetManagementHotelService(hotels, accounts);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public ListManagementHotelsUseCase listManagementHotelsUseCase(
      HotelRepository hotels,
      IdentityAccounts accounts,
      PaginationProperties pagination,
      PlatformTransactionManager transactionManager) {
    ListManagementHotelsService service =
        new ListManagementHotelsService(hotels, accounts, pagination.maxSize());
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

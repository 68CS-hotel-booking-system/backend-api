package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.service.UpdateRoomService;
import com.team.hotelbooking.hotel.application.service.UpdateRoomTypeService;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateRoomService;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateRoomTypeService;
import com.team.hotelbooking.hotel.application.usecase.impl.GetManagementHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListManagementHotelsService;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
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
  public CreateRoomUseCase createRoomUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    CreateRoomService service = new CreateRoomService(hotels, roomTypes, rooms, accounts, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public CreateRoomTypeUseCase createRoomTypeUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    CreateRoomTypeService service = new CreateRoomTypeService(hotels, roomTypes, accounts, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

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

  @Bean
  public UpdateRoomTypeUseCase updateRoomTypeUseCase(
      HotelJpaRepository hotelJpaRepository,
      RoomTypeJpaRepository roomTypeJpaRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    UpdateRoomTypeService service =
        new UpdateRoomTypeService(hotelJpaRepository, roomTypeJpaRepository, clock);
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setReadOnly(false);
    return command -> tx.execute(status -> service.execute(command));
  }

  @Bean
  public UpdateRoomUseCase updateRoomUseCase(
      HotelJpaRepository hotelJpaRepository,
      RoomJpaRepository roomJpaRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    UpdateRoomService service = new UpdateRoomService(hotelJpaRepository, roomJpaRepository, clock);
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setReadOnly(false);
    return command -> tx.execute(status -> service.execute(command));
  }
}

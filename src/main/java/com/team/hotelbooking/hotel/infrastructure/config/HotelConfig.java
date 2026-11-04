package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.service.UpdateRoomService;
import com.team.hotelbooking.hotel.application.service.UpdateRoomTypeService;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class HotelConfig {

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

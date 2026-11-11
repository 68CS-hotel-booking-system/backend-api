package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.usecase.CreateCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.DetachCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetManagementAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.GetRoomTypeAmenitiesService;
import com.team.hotelbooking.hotel.application.usecase.impl.ScopedAmenityService;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.hotel.domain.repository.ScopedAmenityRepository;
import java.time.Clock;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@RequiredArgsConstructor
public class ScopedAmenityConfig {
  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final AmenityRepository amenities;
  private final ScopedAmenityRepository scoped;
  private final Clock clock;
  private final PlatformTransactionManager transactionManager;

  private ScopedAmenityService service() {
    return new ScopedAmenityService(hotels, rooms, amenities, scoped, clock);
  }

  @Bean
  public GetManagementAmenitiesUseCase getManagementAmenitiesUseCase() {
    ScopedAmenityService service = service();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public CreateCustomAmenityUseCase createCustomAmenityUseCase() {
    ScopedAmenityService service = service();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public UpdateCustomAmenityUseCase updateCustomAmenityUseCase() {
    ScopedAmenityService service = service();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public DetachCustomAmenityUseCase detachCustomAmenityUseCase() {
    ScopedAmenityService service = service();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ReplaceRoomAmenitiesUseCase replaceRoomAmenitiesUseCase() {
    ScopedAmenityService service = service();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public GetRoomTypeAmenitiesUseCase getRoomTypeAmenitiesUseCase(RoomTypeRepository roomTypes) {
    GetRoomTypeAmenitiesService service =
        new GetRoomTypeAmenitiesService(hotels, roomTypes, amenities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.port.HotelReviewSnapshotPort;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListHotelReviewsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelReviewsUseCase;
import com.team.hotelbooking.hotel.application.usecase.RejectHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.SubmitHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.ApproveHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.HotelReviewService;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelReviewRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@RequiredArgsConstructor
public class HotelReviewConfig {
  private final HotelRepository hotels;
  private final HotelReviewRepository reviews;
  private final HotelReviewSnapshotPort snapshots;
  private final IdentityAccounts accounts;
  private final RoomTypeRepository roomTypes;
  private final RoomRepository rooms;
  private final HotelStatusHistoryRepository history;
  private final Clock clock;
  private final PaginationProperties pagination;
  private final PlatformTransactionManager manager;

  private HotelReviewService service() {
    return new HotelReviewService(
        hotels,
        reviews,
        snapshots,
        accounts,
        roomTypes,
        rooms,
        history,
        clock,
        pagination.maxSize());
  }

  @Bean
  ApproveHotelUseCase approveHotelUseCase() {
    ApproveHotelService service = new ApproveHotelService(service());
    TransactionTemplate transaction = new TransactionTemplate(manager);
    return (hotelId, adminId) ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(hotelId, adminId)));
  }

  @Bean
  SubmitHotelReviewUseCase submitHotelReviewUseCase() {
    HotelReviewService service = service();
    TransactionTemplate transaction = new TransactionTemplate(manager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  GetHotelReviewUseCase getHotelReviewUseCase() {
    HotelReviewService service = service();
    TransactionTemplate transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  ListHotelReviewsUseCase listHotelReviewsUseCase() {
    HotelReviewService service = service();
    TransactionTemplate transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  ApproveHotelReviewUseCase approveHotelReviewUseCase() {
    HotelReviewService service = service();
    TransactionTemplate transaction = new TransactionTemplate(manager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  ListManagementHotelReviewsUseCase listManagementHotelReviewsUseCase() {
    HotelReviewService service = service();
    TransactionTemplate transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  RejectHotelReviewUseCase rejectHotelReviewUseCase() {
    HotelReviewService service = service();
    TransactionTemplate transaction = new TransactionTemplate(manager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }
}

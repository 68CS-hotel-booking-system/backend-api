package com.team.hotelbooking.booking.infrastructure.config;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.review.application.port.GuestReviewHotelPort;
import com.team.hotelbooking.booking.review.application.usecase.*;
import com.team.hotelbooking.booking.review.application.usecase.impl.GuestReviewService;
import com.team.hotelbooking.booking.review.domain.repository.GuestReviewRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class GuestReviewConfig {
  private GuestReviewService service(
      BookingRepository bookings,
      GuestReviewRepository reviews,
      BookingIdentityPort accounts,
      GuestReviewHotelPort hotels,
      Clock clock,
      PaginationProperties pagination) {
    return new GuestReviewService(bookings, reviews, accounts, hotels, clock, pagination.maxSize());
  }

  @Bean
  SubmitGuestReviewUseCase submitGuestReviewUseCase(
      BookingRepository bookings,
      GuestReviewRepository reviews,
      BookingIdentityPort accounts,
      GuestReviewHotelPort hotels,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(bookings, reviews, accounts, hotels, clock, pagination);
    var tx = new TransactionTemplate(manager);
    return c -> Objects.requireNonNull(tx.execute(s -> service.execute(c)));
  }

  @Bean
  GetBookingGuestReviewUseCase getBookingGuestReviewUseCase(
      BookingRepository bookings,
      GuestReviewRepository reviews,
      BookingIdentityPort accounts,
      GuestReviewHotelPort hotels,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(bookings, reviews, accounts, hotels, clock, pagination);
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return q -> Objects.requireNonNull(tx.execute(s -> service.execute(q)));
  }

  @Bean
  ListGuestReviewsUseCase listGuestReviewsUseCase(
      BookingRepository bookings,
      GuestReviewRepository reviews,
      BookingIdentityPort accounts,
      GuestReviewHotelPort hotels,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(bookings, reviews, accounts, hotels, clock, pagination);
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return q -> Objects.requireNonNull(tx.execute(s -> service.execute(q)));
  }

  @Bean
  ReplyGuestReviewUseCase replyGuestReviewUseCase(
      BookingRepository bookings,
      GuestReviewRepository reviews,
      BookingIdentityPort accounts,
      GuestReviewHotelPort hotels,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(bookings, reviews, accounts, hotels, clock, pagination);
    var tx = new TransactionTemplate(manager);
    return c -> Objects.requireNonNull(tx.execute(s -> service.execute(c)));
  }
}

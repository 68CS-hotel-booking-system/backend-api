package com.team.hotelbooking.booking.infrastructure.config;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.application.usecase.GetCustomerBookingDetailUseCase;
import com.team.hotelbooking.booking.application.usecase.GetManagementBookingDetailUseCase;
import com.team.hotelbooking.booking.application.usecase.ListCustomerBookingsUseCase;
import com.team.hotelbooking.booking.application.usecase.ListManagementBookingsUseCase;
import com.team.hotelbooking.booking.application.usecase.impl.CreateBookingService;
import com.team.hotelbooking.booking.application.usecase.impl.GetCustomerBookingDetailService;
import com.team.hotelbooking.booking.application.usecase.impl.GetManagementBookingDetailService;
import com.team.hotelbooking.booking.application.usecase.impl.ListCustomerBookingsService;
import com.team.hotelbooking.booking.application.usecase.impl.ListManagementBookingsService;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.infrastructure.integration.BookingCheckoutDirectoryAdapter;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class BookingConfig {

  @Bean
  public CreateBookingUseCase createBookingUseCase(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingPricingPort pricing,
      BookingInventoryPort inventory,
      Clock clock,
      @Value("${booking.hold-ttl:PT15M}") String holdTtl,
      PlatformTransactionManager transactionManager) {
    CreateBookingService service =
        new CreateBookingService(
            bookings, accounts, pricing, inventory, clock, Duration.parse(holdTtl));
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ListCustomerBookingsUseCase listCustomerBookingsUseCase(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      PaginationProperties pagination,
      PlatformTransactionManager transactionManager) {
    ListCustomerBookingsService service =
        new ListCustomerBookingsService(bookings, accounts, pagination.maxSize());
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public GetCustomerBookingDetailUseCase getCustomerBookingDetailUseCase(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      PlatformTransactionManager transactionManager) {
    GetCustomerBookingDetailService service =
        new GetCustomerBookingDetailService(bookings, accounts);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public ListManagementBookingsUseCase listManagementBookingsUseCase(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingHotelPort hotels,
      PaginationProperties pagination,
      PlatformTransactionManager transactionManager) {
    ListManagementBookingsService service =
        new ListManagementBookingsService(bookings, accounts, hotels, pagination.maxSize());
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public GetManagementBookingDetailUseCase getManagementBookingDetailUseCase(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingHotelPort hotels,
      PlatformTransactionManager transactionManager) {
    GetManagementBookingDetailService service =
        new GetManagementBookingDetailService(bookings, accounts, hotels);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public BookingCheckoutDirectory bookingCheckoutDirectory(
      BookingRepository bookings, PlatformTransactionManager transactionManager) {
    BookingCheckoutDirectoryAdapter adapter = new BookingCheckoutDirectoryAdapter(bookings);
    TransactionTemplate readOnlyTx = new TransactionTemplate(transactionManager);
    readOnlyTx.setReadOnly(true);
    TransactionTemplate writeTx = new TransactionTemplate(transactionManager);

    return new BookingCheckoutDirectory() {
      @Override
      public Optional<BookingCheckoutSnapshot> findById(UUID bookingId) {
        return Objects.requireNonNull(readOnlyTx.execute(status -> adapter.findById(bookingId)));
      }

      @Override
      public boolean updateStayStatus(UUID bookingId, String status, Instant updatedAt) {
        Boolean result =
            writeTx.execute(txStatus -> adapter.updateStayStatus(bookingId, status, updatedAt));
        return Boolean.TRUE.equals(result);
      }
    };
  }
}

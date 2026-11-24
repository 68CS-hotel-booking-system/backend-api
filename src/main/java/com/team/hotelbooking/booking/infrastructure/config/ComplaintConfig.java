package com.team.hotelbooking.booking.infrastructure.config;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.complaint.application.port.ComplaintHotelPort;
import com.team.hotelbooking.booking.complaint.application.usecase.*;
import com.team.hotelbooking.booking.complaint.application.usecase.GetMyComplaintUseCase;
import com.team.hotelbooking.booking.complaint.application.usecase.ListMyComplaintsUseCase;
import com.team.hotelbooking.booking.complaint.application.usecase.SubmitComplaintUseCase;
import com.team.hotelbooking.booking.complaint.application.usecase.impl.*;
import com.team.hotelbooking.booking.complaint.application.usecase.impl.GetMyComplaintService;
import com.team.hotelbooking.booking.complaint.application.usecase.impl.ListMyComplaintsService;
import com.team.hotelbooking.booking.complaint.application.usecase.impl.SubmitComplaintService;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintCaseRepository;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintRepository;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// BK-21 tách khỏi BookingConfig: khiếu nại chỉ đọc booking qua BookingRepository.
@Configuration
public class ComplaintConfig {

  // Kiểm booking, kiểm khiếu nại đang mở và chèn trong cùng transaction.
  @Bean
  public SubmitComplaintUseCase submitComplaintUseCase(
      BookingRepository bookingRepository,
      ComplaintRepository complaintRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    SubmitComplaintService service =
        new SubmitComplaintService(bookingRepository, complaintRepository, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ListMyComplaintsUseCase listMyComplaintsUseCase(
      ComplaintRepository complaintRepository, PlatformTransactionManager transactionManager) {
    ListMyComplaintsService service = new ListMyComplaintsService(complaintRepository);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public GetMyComplaintUseCase getMyComplaintUseCase(
      ComplaintRepository complaintRepository,
      ComplaintCaseRepository cases,
      PlatformTransactionManager transactionManager) {
    GetMyComplaintService service = new GetMyComplaintService(complaintRepository, cases);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public ListManagedComplaintsUseCase listManagedComplaintsUseCase(
      ComplaintCaseRepository cases,
      BookingIdentityPort accounts,
      ComplaintHotelPort hotels,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = new ListManagedComplaintsService(cases, accounts, hotels, pagination.maxSize());
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return query -> Objects.requireNonNull(tx.execute(status -> service.execute(query)));
  }

  @Bean
  public GetManagedComplaintUseCase getManagedComplaintUseCase(
      ComplaintCaseRepository cases,
      BookingIdentityPort accounts,
      ComplaintHotelPort hotels,
      PlatformTransactionManager manager) {
    var service = new GetManagedComplaintService(cases, accounts, hotels);
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return query -> Objects.requireNonNull(tx.execute(status -> service.execute(query)));
  }

  @Bean
  public HandleComplaintUseCase handleComplaintUseCase(
      ComplaintCaseRepository cases,
      BookingIdentityPort accounts,
      ComplaintHotelPort hotels,
      Clock clock,
      PlatformTransactionManager manager) {
    var service = new HandleComplaintService(cases, accounts, hotels, clock);
    var tx = new TransactionTemplate(manager);
    return command -> Objects.requireNonNull(tx.execute(status -> service.execute(command)));
  }
}

package com.team.hotelbooking.payment.infrastructure.config;

import com.team.hotelbooking.payment.application.port.*;
import com.team.hotelbooking.payment.application.usecase.*;
import com.team.hotelbooking.payment.application.usecase.impl.*;
import com.team.hotelbooking.payment.domain.repository.RefundRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RefundConfig {
  @Bean
  RefundRegistrationPort refundRegistrationPort(RefundRepository refunds) {
    return new RefundRegistrationService(refunds);
  }

  private RefundService service(
      RefundRepository refunds,
      RefundAccessPort access,
      Clock clock,
      PaginationProperties pagination) {
    return new RefundService(refunds, access, clock, pagination.maxSize());
  }

  @Bean
  ListRefundsUseCase listRefundsUseCase(
      RefundRepository refunds,
      RefundAccessPort access,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(refunds, access, clock, pagination);
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return q -> Objects.requireNonNull(tx.execute(s -> service.execute(q)));
  }

  @Bean
  GetRefundUseCase getRefundUseCase(
      RefundRepository refunds,
      RefundAccessPort access,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(refunds, access, clock, pagination);
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return q -> Objects.requireNonNull(tx.execute(s -> service.execute(q)));
  }

  @Bean
  ProcessRefundUseCase processRefundUseCase(
      RefundRepository refunds,
      RefundAccessPort access,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager manager) {
    var service = service(refunds, access, clock, pagination);
    var tx = new TransactionTemplate(manager);
    return q -> Objects.requireNonNull(tx.execute(s -> service.execute(q)));
  }
}

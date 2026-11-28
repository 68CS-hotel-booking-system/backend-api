package com.team.hotelbooking.payment.infrastructure.config;

import com.team.hotelbooking.payment.application.port.FinanceDashboardPort;
import com.team.hotelbooking.payment.application.usecase.GetFinanceDashboardUseCase;
import com.team.hotelbooking.payment.application.usecase.impl.GetFinanceDashboardService;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class FinanceDashboardConfig {
  @Bean
  public GetFinanceDashboardUseCase getFinanceDashboardUseCase(
      FinanceDashboardPort port, Clock clock, PlatformTransactionManager manager) {
    var service = new GetFinanceDashboardService(port, clock);
    var transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

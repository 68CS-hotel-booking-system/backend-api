package com.team.hotelbooking.payment.infrastructure.config;

import com.team.hotelbooking.payment.ContractDirectory;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.service.CreateContractService;
import com.team.hotelbooking.payment.application.service.GetContractsService;
import com.team.hotelbooking.payment.application.service.GetFinancialAccessService;
import com.team.hotelbooking.payment.application.service.UpdateFinancialAccessService;
import com.team.hotelbooking.payment.application.usecase.ActivateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.application.usecase.GetFinancialAccessUseCase;
import com.team.hotelbooking.payment.application.usecase.TerminateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.UpdateFinancialAccessUseCase;
import com.team.hotelbooking.payment.application.usecase.impl.ActivateContractService;
import com.team.hotelbooking.payment.application.usecase.impl.TerminateContractService;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import com.team.hotelbooking.payment.infrastructure.integration.PaymentContractDirectoryAdapter;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessHistoryJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class PaymentConfig {

  @Bean
  public CreateContractUseCase createContractUseCase(
      HotelContractRepository contractRepository,
      PaymentHotelPort paymentHotelPort,
      PaymentIdentityPort paymentIdentityPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CreateContractService service =
        new CreateContractService(contractRepository, paymentHotelPort, paymentIdentityPort, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  // Khóa hợp đồng, kiểm chồng hiệu lực và ghi trạng thái trong cùng transaction.
  @Bean
  public ActivateContractUseCase activateContractUseCase(
      HotelContractRepository contractRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    ActivateContractService service = new ActivateContractService(contractRepository, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public TerminateContractUseCase terminateContractUseCase(
      HotelContractRepository contractRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    TerminateContractService service = new TerminateContractService(contractRepository, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  // Không bọc transaction riêng: module gọi (booking) tham gia transaction của chính nó.
  @Bean
  public ContractDirectory contractDirectory(HotelContractRepository contractRepository) {
    return new PaymentContractDirectoryAdapter(contractRepository);
  }

  @Bean
  public GetContractsUseCase getContractsUseCase(
      HotelContractJpaRepository contractJpaRepository,
      PaymentHotelPort paymentHotelPort,
      HotelFinancialAccessJpaRepository financialAccessJpaRepository,
      PlatformTransactionManager transactionManager) {

    GetContractsService service =
        new GetContractsService(
            contractJpaRepository, paymentHotelPort, financialAccessJpaRepository);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public UpdateFinancialAccessUseCase updateFinancialAccessUseCase(
      HotelFinancialAccessJpaRepository accessJpaRepository,
      HotelFinancialAccessHistoryJpaRepository historyJpaRepository,
      PaymentHotelPort paymentHotelPort,
      PaymentIdentityPort paymentIdentityPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    UpdateFinancialAccessService service =
        new UpdateFinancialAccessService(
            accessJpaRepository,
            historyJpaRepository,
            paymentHotelPort,
            paymentIdentityPort,
            clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public GetFinancialAccessUseCase getFinancialAccessUseCase(
      HotelFinancialAccessJpaRepository accessJpaRepository,
      HotelFinancialAccessHistoryJpaRepository historyJpaRepository,
      PaymentHotelPort paymentHotelPort,
      PaymentIdentityPort paymentIdentityPort,
      PlatformTransactionManager transactionManager) {

    GetFinancialAccessService service =
        new GetFinancialAccessService(
            accessJpaRepository, historyJpaRepository, paymentHotelPort, paymentIdentityPort);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

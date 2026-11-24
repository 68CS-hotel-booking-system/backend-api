package com.team.hotelbooking.payment.infrastructure.config;

import com.team.hotelbooking.payment.ContractDirectory;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.service.CreateContractService;
import com.team.hotelbooking.payment.application.service.GetContractsService;
import com.team.hotelbooking.payment.application.usecase.ActivateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.application.usecase.TerminateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.impl.ActivateContractService;
import com.team.hotelbooking.payment.application.usecase.impl.TerminateContractService;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import com.team.hotelbooking.payment.infrastructure.integration.PaymentContractDirectoryAdapter;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(VnpayProperties.class)
public class PaymentConfig {
  @Bean
  public com.team.hotelbooking.payment.application.usecase.ListPartnerCustomersUseCase
      listPartnerCustomersUseCase(
          com.team.hotelbooking.payment.application.port.PartnerCustomerPort customers,
          Clock clock,
          com.team.hotelbooking.shared.config.PaginationProperties pagination,
          PlatformTransactionManager manager) {
    var service =
        new com.team.hotelbooking.payment.application.usecase.impl.ListPartnerCustomersService(
            customers, clock, pagination.maxSize());
    var transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public com.team.hotelbooking.payment.application.usecase.RenewContractUseCase
      renewContractUseCase(
          HotelContractRepository contracts,
          CreateContractUseCase create,
          PlatformTransactionManager manager) {
    var service =
        new com.team.hotelbooking.payment.application.usecase.impl.RenewContractService(
            contracts, create);
    var transaction = new TransactionTemplate(manager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

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
      Clock clock,
      PlatformTransactionManager transactionManager) {

    GetContractsService service =
        new GetContractsService(contractJpaRepository, paymentHotelPort, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}

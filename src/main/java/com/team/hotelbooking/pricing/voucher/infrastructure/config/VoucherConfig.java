package com.team.hotelbooking.pricing.voucher.infrastructure.config;

import com.team.hotelbooking.pricing.VoucherDirectory;
import com.team.hotelbooking.pricing.voucher.application.port.VoucherAccessPort;
import com.team.hotelbooking.pricing.voucher.application.usecase.*;
import com.team.hotelbooking.pricing.voucher.application.usecase.impl.VoucherService;
import com.team.hotelbooking.pricing.voucher.domain.exception.VoucherException;
import com.team.hotelbooking.pricing.voucher.domain.repository.VoucherRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class VoucherConfig {
  @Bean
  public VoucherService voucherService(
      VoucherRepository repository,
      VoucherAccessPort access,
      Clock clock,
      PaginationProperties page) {
    return new VoucherService(repository, access, clock, page.maxSize());
  }

  @Bean
  public CreateVoucherUseCase createVoucherUseCase(
      VoucherService service, PlatformTransactionManager manager) {
    var tx = new TransactionTemplate(manager);
    return c -> Objects.requireNonNull(tx.execute(s -> service.execute(c)));
  }

  @Bean
  public ListVouchersUseCase listVouchersUseCase(
      VoucherService service, PlatformTransactionManager manager) {
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    return q -> Objects.requireNonNull(tx.execute(s -> service.execute(q)));
  }

  @Bean
  public SetVoucherEnabledUseCase setVoucherEnabledUseCase(
      VoucherService service, PlatformTransactionManager manager) {
    var tx = new TransactionTemplate(manager);
    return c -> Objects.requireNonNull(tx.execute(s -> service.execute(c)));
  }

  @Bean
  public VoucherDirectory voucherDirectory(
      VoucherService service, PlatformTransactionManager manager) {
    // Các thao tác giữ/dùng/trả lượt phải tham gia transaction của booking/thanh toán.
    var write = new TransactionTemplate(manager);
    write.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    var read = new TransactionTemplate(manager);
    read.setReadOnly(true);
    return new VoucherDirectory() {
      public long discount(UUID h, String c, long amount) {
        return call(
            () -> Objects.requireNonNull(read.execute(s -> service.discount(h, c, amount))));
      }

      public void reserve(UUID h, String c, UUID b, long amount, long discount, Instant expiry) {
        call(
            () -> {
              write.executeWithoutResult(s -> service.reserve(h, c, b, amount, discount, expiry));
              return null;
            });
      }

      public void lockForPayment(UUID b) {
        write.executeWithoutResult(s -> service.lockForPayment(b));
      }

      public void consume(UUID b) {
        write.executeWithoutResult(s -> service.consume(b));
      }

      public void release(UUID b) {
        write.executeWithoutResult(s -> service.release(b));
      }

      public String bookingCode(UUID b) {
        return read.execute(s -> service.bookingCode(b));
      }

      private <T> T call(java.util.function.Supplier<T> action) {
        try {
          return action.get();
        } catch (VoucherException e) {
          throw new VoucherUnavailableException(e.getMessage());
        }
      }
    };
  }
}

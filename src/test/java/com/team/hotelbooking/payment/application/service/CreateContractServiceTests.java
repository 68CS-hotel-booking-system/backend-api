package com.team.hotelbooking.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ tạo hợp đồng hoa hồng")
class CreateContractServiceTests {

  @Mock private HotelContractJpaRepository contractJpaRepository;
  @Mock private PaymentHotelPort paymentHotelPort;
  @Mock private PaymentIdentityPort paymentIdentityPort;

  private Clock clock;
  private CreateContractService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);
    service =
        new CreateContractService(
            contractJpaRepository, paymentHotelPort, paymentIdentityPort, clock);
  }

  @Nested
  @DisplayName("CreateContractUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Ném lỗi khi khách sạn hoặc chủ sở hữu không tồn tại")
    void shouldThrowWhenHotelOrUserNotFound() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      CreateContractCommand command =
          new CreateContractCommand(
              hotelId,
              "HD-01",
              ownerId,
              new BigDecimal("10.00"),
              HotelContract.CommissionBasis.AFTER_DISCOUNT,
              Instant.now(clock),
              Instant.now(clock).plusSeconds(3600),
              HotelContract.ContractStatus.DRAFT,
              UUID.randomUUID());

      given(paymentHotelPort.existsById(hotelId)).willReturn(false);

      // When & Then
      FinanceException error = assertThrows(FinanceException.class, () -> service.execute(command));
      assertEquals(FinanceErrorCode.HOTEL_NOT_FOUND, error.code());

      // Given
      given(paymentHotelPort.existsById(hotelId)).willReturn(true);
      given(paymentIdentityPort.existsById(ownerId)).willReturn(false);

      // When & Then
      FinanceException userError =
          assertThrows(FinanceException.class, () -> service.execute(command));
      assertEquals(FinanceErrorCode.USER_NOT_FOUND, userError.code());
    }

    @Test
    @DisplayName("Tạo hợp đồng thành công khi dữ liệu hợp lệ")
    void shouldCreateContractSuccessfully() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      Instant start = Instant.now(clock);
      Instant end = start.plusSeconds(86400 * 30);

      CreateContractCommand command =
          new CreateContractCommand(
              hotelId,
              "HD-SUCCESS-01",
              ownerId,
              new BigDecimal("15.50"),
              HotelContract.CommissionBasis.AFTER_DISCOUNT,
              start,
              end,
              HotelContract.ContractStatus.DRAFT,
              UUID.randomUUID());

      given(paymentHotelPort.existsById(hotelId)).willReturn(true);
      given(paymentIdentityPort.existsById(ownerId)).willReturn(true);
      given(contractJpaRepository.existsByContractCode("HD-SUCCESS-01")).willReturn(false);

      HotelContractJpaEntity entity = new HotelContractJpaEntity();
      entity.setId(UUID.randomUUID());
      entity.setContractCode("HD-SUCCESS-01");
      entity.setHotelId(hotelId);
      entity.setOwnerUserId(ownerId);
      entity.setCommissionRate(new BigDecimal("15.50"));
      entity.setCommissionBasis(HotelContractJpaEntity.DbCommissionBasis.AFTER_DISCOUNT);
      entity.setStartsAt(start);
      entity.setEndsAt(end);
      entity.setStatus(HotelContractJpaEntity.DbContractStatus.DRAFT);
      entity.setCreatedByUserId(UUID.randomUUID());
      entity.setCreatedAt(start);
      entity.setUpdatedAt(start);

      given(contractJpaRepository.save(any())).willReturn(entity);

      // When
      HotelContractResult result = service.execute(command);

      // Then
      assertThat(result.contractCode()).isEqualTo("HD-SUCCESS-01");
      assertThat(result.commissionRate()).isEqualTo(new BigDecimal("15.50"));
      assertThat(result.status()).isEqualTo(HotelContract.ContractStatus.DRAFT);
    }
  }
}

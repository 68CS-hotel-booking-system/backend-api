package com.team.hotelbooking.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ tạo hợp đồng hoa hồng")
class CreateContractServiceTests {

  private static final UUID ADMIN_ID = UUID.fromString("3c000000-0000-4000-8000-000000000001");

  @Mock private HotelContractRepository contractRepository;
  @Mock private PaymentHotelPort paymentHotelPort;
  @Mock private PaymentIdentityPort paymentIdentityPort;

  private Clock clock;
  private CreateContractService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);
    service =
        new CreateContractService(contractRepository, paymentHotelPort, paymentIdentityPort, clock);
  }

  private CreateContractCommand command(
      UUID hotelId, UUID ownerId, String code, HotelContract.ContractStatus status, UUID adminId) {
    Instant start = Instant.now(clock);
    return new CreateContractCommand(
        hotelId,
        code,
        ownerId,
        new BigDecimal("15.50"),
        HotelContract.CommissionBasis.AFTER_DISCOUNT,
        start,
        start.plusSeconds(86400 * 30),
        status,
        adminId);
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
          command(hotelId, ownerId, "HD-01", HotelContract.ContractStatus.DRAFT, ADMIN_ID);

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
    @DisplayName("Tạo hợp đồng thành công, người tạo là ADMIN đang đăng nhập")
    void shouldCreateContractWithAdminAsCreatorWhenDataIsValid() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      given(paymentHotelPort.existsById(hotelId)).willReturn(true);
      given(paymentIdentityPort.existsById(ownerId)).willReturn(true);
      given(contractRepository.existsByContractCode("HD-SUCCESS-01")).willReturn(false);
      given(contractRepository.add(any()))
          .willAnswer(invocation -> withId(invocation.getArgument(0)));
      ArgumentCaptor<HotelContract> saved = ArgumentCaptor.forClass(HotelContract.class);

      // When
      HotelContractResult result =
          service.execute(
              command(
                  hotelId,
                  ownerId,
                  " HD-SUCCESS-01 ",
                  HotelContract.ContractStatus.DRAFT,
                  ADMIN_ID));

      // Then
      assertThat(result.contractCode()).isEqualTo("HD-SUCCESS-01");
      assertThat(result.commissionRate()).isEqualTo(new BigDecimal("15.50"));
      assertThat(result.status()).isEqualTo(HotelContract.ContractStatus.DRAFT);
      assertThat(result.createdByUserId()).isEqualTo(ADMIN_ID);
      then(contractRepository).should().add(saved.capture());
      assertThat(saved.getValue().createdByUserId()).isEqualTo(ADMIN_ID);
      assertThat(saved.getValue().createdAt()).isEqualTo(Instant.now(clock));
      then(contractRepository).should(never()).hasOverlappingActive(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Từ chối và không ghi khi thiếu người tạo (không tự sinh UUID thay thế)")
    void shouldRejectWithoutSavingWhenCreatorIsMissing() {
      // When & Then
      FinanceException error =
          assertThrows(
              FinanceException.class,
              () ->
                  service.execute(
                      command(
                          UUID.randomUUID(),
                          UUID.randomUUID(),
                          "HD-02",
                          HotelContract.ContractStatus.DRAFT,
                          null)));
      assertEquals(FinanceErrorCode.ACCESS_DENIED, error.code());
      then(contractRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả 409 khi tạo thẳng ACTIVE chồng hiệu lực với hợp đồng ACTIVE khác")
    void shouldRejectWhenCreatingActiveContractThatOverlaps() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      given(paymentHotelPort.existsById(hotelId)).willReturn(true);
      given(paymentIdentityPort.existsById(ownerId)).willReturn(true);
      given(contractRepository.existsByContractCode("HD-03")).willReturn(false);
      given(contractRepository.hasOverlappingActive(any(), any(), any(), any())).willReturn(true);

      // When & Then
      FinanceException error =
          assertThrows(
              FinanceException.class,
              () ->
                  service.execute(
                      command(
                          hotelId,
                          ownerId,
                          "HD-03",
                          HotelContract.ContractStatus.ACTIVE,
                          ADMIN_ID)));
      assertEquals(FinanceErrorCode.ACTIVE_CONTRACT_PERIOD_OVERLAP, error.code());
      then(contractRepository).should(never()).add(any());
    }
  }

  private static HotelContract withId(HotelContract contract) {
    return new HotelContract(
        UUID.randomUUID(),
        contract.contractCode(),
        contract.hotelId(),
        contract.ownerUserId(),
        contract.commissionRate(),
        contract.commissionBasis(),
        contract.startsAt(),
        contract.endsAt(),
        contract.status(),
        contract.createdByUserId(),
        contract.createdAt(),
        contract.updatedAt());
  }
}

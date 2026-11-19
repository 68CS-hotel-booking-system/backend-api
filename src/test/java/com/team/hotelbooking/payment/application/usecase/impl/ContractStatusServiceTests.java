package com.team.hotelbooking.payment.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
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
@DisplayName("Dịch vụ kích hoạt và chấm dứt hợp đồng (PM-16)")
class ContractStatusServiceTests {

  private static final UUID ADMIN_ID = UUID.fromString("3d000000-0000-4000-8000-000000000001");
  private static final UUID HOTEL_ID = UUID.fromString("3d000000-0000-4000-8000-000000000101");
  private static final UUID CONTRACT_ID = UUID.fromString("3d000000-0000-4000-8000-000000000201");
  private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
  private static final ChangeContractStatusCommand COMMAND =
      new ChangeContractStatusCommand(ADMIN_ID, HOTEL_ID, CONTRACT_ID);

  @Mock private HotelContractRepository contracts;

  private ActivateContractService activateService;
  private TerminateContractService terminateService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    activateService = new ActivateContractService(contracts, clock);
    terminateService = new TerminateContractService(contracts, clock);
  }

  private static HotelContract contract(HotelContract.ContractStatus status, Instant endsAt) {
    return new HotelContract(
        CONTRACT_ID,
        "HD-PM16",
        HOTEL_ID,
        UUID.fromString("3d000000-0000-4000-8000-000000000301"),
        new BigDecimal("12.50"),
        HotelContract.CommissionBasis.BEFORE_DISCOUNT,
        Instant.parse("2026-12-01T00:00:00Z"),
        endsAt,
        status,
        ADMIN_ID,
        Instant.parse("2026-11-01T00:00:00Z"),
        Instant.parse("2026-11-01T00:00:00Z"));
  }

  private void givenLocked(HotelContract contract) {
    given(contracts.findByIdAndHotelIdForUpdate(CONTRACT_ID, HOTEL_ID))
        .willReturn(Optional.of(contract));
  }

  private static FinanceErrorCode codeOf(Throwable exception) {
    return ((FinanceException) exception).code();
  }

  @Nested
  @DisplayName("ActivateContractUseCase.execute")
  class ActivateTests {

    @Test
    @DisplayName("Kích hoạt DRAFT, chỉ đổi trạng thái và updatedAt, giữ nguyên điều khoản")
    void shouldActivateDraftAndKeepTermsWhenNoOverlap() {
      // Given
      HotelContract draft =
          contract(HotelContract.ContractStatus.DRAFT, Instant.parse("2027-12-01T00:00:00Z"));
      givenLocked(draft);
      given(contracts.hasOverlappingActive(HOTEL_ID, draft.startsAt(), draft.endsAt(), CONTRACT_ID))
          .willReturn(false);
      given(contracts.updateStatus(any())).willAnswer(invocation -> invocation.getArgument(0));
      ArgumentCaptor<HotelContract> updated = ArgumentCaptor.forClass(HotelContract.class);

      // When
      HotelContractResult result = activateService.execute(COMMAND);

      // Then
      assertThat(result.status()).isEqualTo(HotelContract.ContractStatus.ACTIVE);
      then(contracts).should().updateStatus(updated.capture());
      assertThat(updated.getValue().updatedAt()).isEqualTo(NOW);
      assertThat(updated.getValue().commissionRate()).isEqualTo(new BigDecimal("12.50"));
      assertThat(updated.getValue().startsAt()).isEqualTo(draft.startsAt());
      assertThat(updated.getValue().endsAt()).isEqualTo(draft.endsAt());
    }

    @Test
    @DisplayName("Trả 409 và không ghi khi chồng hiệu lực với hợp đồng ACTIVE khác")
    void shouldRejectWithoutWritingWhenPeriodOverlaps() {
      // Given
      HotelContract draft =
          contract(HotelContract.ContractStatus.DRAFT, Instant.parse("2027-12-01T00:00:00Z"));
      givenLocked(draft);
      given(contracts.hasOverlappingActive(any(), any(), any(), any())).willReturn(true);

      // When & Then
      assertThatThrownBy(() -> activateService.execute(COMMAND))
          .satisfies(
              ex ->
                  assertThat(codeOf(ex))
                      .isEqualTo(FinanceErrorCode.ACTIVE_CONTRACT_PERIOD_OVERLAP));
      then(contracts).should(never()).updateStatus(any());
    }

    @Test
    @DisplayName("Trả 409 khi hợp đồng không ở DRAFT, 400 khi đã hết hiệu lực")
    void shouldRejectWhenNotDraftOrAlreadyEnded() {
      // Given
      givenLocked(
          contract(HotelContract.ContractStatus.ACTIVE, Instant.parse("2027-12-01T00:00:00Z")));

      // When & Then
      assertThatThrownBy(() -> activateService.execute(COMMAND))
          .satisfies(
              ex ->
                  assertThat(codeOf(ex))
                      .isEqualTo(FinanceErrorCode.INVALID_CONTRACT_STATUS_TRANSITION));

      // Given
      givenLocked(contract(HotelContract.ContractStatus.DRAFT, NOW));

      // When & Then
      assertThatThrownBy(() -> activateService.execute(COMMAND))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(FinanceErrorCode.INVALID_CONTRACT_PERIOD));
      then(contracts).should(never()).updateStatus(any());
    }

    @Test
    @DisplayName("Trả 404 khi hợp đồng không thuộc khách sạn trên đường dẫn")
    void shouldThrowContractNotFoundWhenContractBelongsToAnotherHotel() {
      // Given
      given(contracts.findByIdAndHotelIdForUpdate(CONTRACT_ID, HOTEL_ID))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> activateService.execute(COMMAND))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(FinanceErrorCode.CONTRACT_NOT_FOUND));
    }
  }

  @Nested
  @DisplayName("TerminateContractUseCase.execute")
  class TerminateTests {

    @Test
    @DisplayName("Chấm dứt hợp đồng ACTIVE, không đổi khoảng hiệu lực")
    void shouldTerminateActiveContractAndKeepPeriod() {
      // Given
      HotelContract active =
          contract(HotelContract.ContractStatus.ACTIVE, Instant.parse("2027-12-01T00:00:00Z"));
      givenLocked(active);
      given(contracts.updateStatus(any())).willAnswer(invocation -> invocation.getArgument(0));

      // When
      HotelContractResult result = terminateService.execute(COMMAND);

      // Then
      assertThat(result.status()).isEqualTo(HotelContract.ContractStatus.TERMINATED);
      assertThat(result.endsAt()).isEqualTo(active.endsAt());
      assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Trả 409 khi hợp đồng đã chấm dứt")
    void shouldRejectWhenAlreadyTerminated() {
      // Given
      givenLocked(
          contract(HotelContract.ContractStatus.TERMINATED, Instant.parse("2027-12-01T00:00:00Z")));

      // When & Then
      assertThatThrownBy(() -> terminateService.execute(COMMAND))
          .satisfies(
              ex ->
                  assertThat(codeOf(ex))
                      .isEqualTo(FinanceErrorCode.INVALID_CONTRACT_STATUS_TRANSITION));
      then(contracts).should(never()).updateStatus(any());
    }

    @Test
    @DisplayName("Từ chối khi thiếu người thực hiện, không khóa hàng")
    void shouldRejectWithoutLockingWhenActorMissing() {
      assertThatThrownBy(
              () ->
                  terminateService.execute(
                      new ChangeContractStatusCommand(null, HOTEL_ID, CONTRACT_ID)))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(FinanceErrorCode.ACCESS_DENIED));
      then(contracts).shouldHaveNoInteractions();
    }
  }
}

package com.team.hotelbooking.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.payment.application.command.UpdateFinancialAccessCommand;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.result.FinancialAccessResult;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessHistoryJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessHistoryJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
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
@DisplayName("Dịch vụ cập nhật phân quyền tài chính")
class UpdateFinancialAccessServiceTests {

  @Mock private HotelFinancialAccessJpaRepository accessJpaRepository;
  @Mock private HotelFinancialAccessHistoryJpaRepository historyJpaRepository;
  @Mock private PaymentHotelPort paymentHotelPort;
  @Mock private PaymentIdentityPort paymentIdentityPort;

  private Clock clock;
  private UpdateFinancialAccessService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);
    service =
        new UpdateFinancialAccessService(
            accessJpaRepository,
            historyJpaRepository,
            paymentHotelPort,
            paymentIdentityPort,
            clock);
  }

  @Nested
  @DisplayName("UpdateFinancialAccessUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Từ chối khi thiếu người thực hiện, không tự sinh định danh và không ghi dữ liệu")
    void shouldThrowAccessDeniedWhenAdminActorIsMissing() {
      // Given
      UpdateFinancialAccessCommand command =
          new UpdateFinancialAccessCommand(
              UUID.randomUUID(), UUID.randomUUID(), true, "FINANCE_READ", "Lý do", 0L, null);

      // When
      FinanceException error = assertThrows(FinanceException.class, () -> service.execute(command));

      // Then
      assertEquals(FinanceErrorCode.ACCESS_DENIED, error.code());
      then(paymentHotelPort).shouldHaveNoInteractions();
      then(paymentIdentityPort).shouldHaveNoInteractions();
      then(accessJpaRepository).shouldHaveNoInteractions();
      then(historyJpaRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném lỗi khi khách sạn hoặc người dùng không tồn tại")
    void shouldThrowWhenHotelOrUserNotFound() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UpdateFinancialAccessCommand command =
          new UpdateFinancialAccessCommand(
              hotelId, userId, true, "FINANCE_READ", "Lý do", 0L, UUID.randomUUID());

      given(paymentHotelPort.existsById(hotelId)).willReturn(false);

      // When & Then
      FinanceException error = assertThrows(FinanceException.class, () -> service.execute(command));
      assertEquals(FinanceErrorCode.HOTEL_NOT_FOUND, error.code());

      // Given
      given(paymentHotelPort.existsById(hotelId)).willReturn(true);
      given(paymentIdentityPort.existsById(userId)).willReturn(false);

      // When & Then
      FinanceException userError =
          assertThrows(FinanceException.class, () -> service.execute(command));
      assertEquals(FinanceErrorCode.USER_NOT_FOUND, userError.code());
    }

    @Test
    @DisplayName("Cấp quyền tài chính mới thành công khi bản ghi chưa tồn tại")
    void shouldCreateNewAccessSuccessfully() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UUID adminId = UUID.randomUUID();
      UpdateFinancialAccessCommand command =
          new UpdateFinancialAccessCommand(
              hotelId, userId, true, "FINANCE_READ", "Cấp quyền", 0L, adminId);

      given(paymentHotelPort.existsById(hotelId)).willReturn(true);
      given(paymentIdentityPort.existsById(userId)).willReturn(true);
      given(accessJpaRepository.findByHotelIdAndUserId(hotelId, userId))
          .willReturn(Optional.empty());

      HotelFinancialAccessJpaEntity entity = new HotelFinancialAccessJpaEntity();
      entity.setId(UUID.randomUUID());
      entity.setHotelId(hotelId);
      entity.setUserId(userId);
      entity.setGranted(true);
      entity.setAccessScope("FINANCE_READ");
      entity.setReason("Cấp quyền");
      entity.setGrantedByUserId(adminId);
      entity.setGrantedAt(Instant.now(clock));
      entity.setVersion(0L);

      given(accessJpaRepository.save(any())).willReturn(entity);
      ArgumentCaptor<HotelFinancialAccessJpaEntity> savedAccess =
          ArgumentCaptor.forClass(HotelFinancialAccessJpaEntity.class);
      ArgumentCaptor<HotelFinancialAccessHistoryJpaEntity> savedHistory =
          ArgumentCaptor.forClass(HotelFinancialAccessHistoryJpaEntity.class);

      // When
      FinancialAccessResult result = service.execute(command);

      // Then
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.granted()).isTrue();
      assertThat(result.accessScope()).isEqualTo("FINANCE_READ");
      assertThat(result.grantedByUserId()).isEqualTo(adminId);

      then(accessJpaRepository).should().save(savedAccess.capture());
      assertThat(savedAccess.getValue().getGrantedByUserId()).isEqualTo(adminId);
      then(historyJpaRepository).should().save(savedHistory.capture());
      assertThat(savedHistory.getValue().getChangedByUserId()).isEqualTo(adminId);
    }
  }
}

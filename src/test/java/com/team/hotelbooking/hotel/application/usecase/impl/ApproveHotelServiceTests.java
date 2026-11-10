package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelPublishRequirement;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ duyệt công khai khách sạn lần đầu (HT-03, HT-09)")
class ApproveHotelServiceTests extends HotelStatusServiceFixture {

  @Mock private IdentityAccounts accounts;

  private ApproveHotelService service;

  @BeforeEach
  void setUp() {
    service = new ApproveHotelService(hotels, roomTypes, rooms, history, accounts, clock);
  }

  private void givenActiveAdmin() {
    given(accounts.findById(ADMIN_ID))
        .willReturn(Optional.of(new AccountSnapshot(ADMIN_ID, true, true, Set.of("ADMIN"))));
  }

  @Nested
  @DisplayName("ApproveHotelUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Chuyển DRAFT sang ACTIVE và ghi lịch sử khi đủ điều kiện HT-03")
    void shouldApproveAndRecordHistoryWhenHotelIsReady() {
      // Given
      givenActiveAdmin();
      givenLockedRow(hotel(Hotel.Status.DRAFT));
      givenSellableInventory(2);
      givenStatusUpdateReturnsSavedHotel();
      ArgumentCaptor<HotelStatusChange> change = ArgumentCaptor.forClass(HotelStatusChange.class);

      // When
      ManagementHotelResult result = service.execute(HOTEL_ID, ADMIN_ID);

      // Then
      assertThat(result.status()).isEqualTo("ACTIVE");
      assertThat(result.updatedAt()).isEqualTo(NOW);
      then(history).should().add(change.capture());
      assertThat(change.getValue().fromStatus()).isEqualTo(Hotel.Status.DRAFT);
      assertThat(change.getValue().toStatus()).isEqualTo(Hotel.Status.ACTIVE);
      assertThat(change.getValue().changedByUserId()).isEqualTo(ADMIN_ID);
      assertThat(change.getValue().reason()).isNull();
      assertThat(change.getValue().changedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Trả 422 HOTEL_NOT_READY kèm mọi lý do và không ghi gì khi thiếu điều kiện")
    void shouldRejectWithAllReasonsWhenHotelIsNotReady() {
      // Given
      givenActiveAdmin();
      givenLockedRow(hotel(Hotel.Status.DRAFT, null));
      givenSellableInventory(0);

      // When & Then
      assertThatThrownBy(() -> service.execute(HOTEL_ID, ADMIN_ID))
          .isInstanceOf(HotelException.class)
          .satisfies(
              ex -> {
                assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_READY);
                assertThat(((HotelException) ex).unmetRequirements())
                    .containsExactly(
                        HotelPublishRequirement.CONTACT, HotelPublishRequirement.OPERATIONAL_ROOM);
              });
      then(hotels).should(never()).updateStatus(any());
      then(history).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Giữ lỗi trạng thái của develop, không kiểm HT-03 khi khách sạn không còn DRAFT")
    void shouldRejectWithoutReadinessCheckWhenHotelIsAlreadyActive() {
      // Given
      givenActiveAdmin();
      givenLockedRow(hotel(Hotel.Status.ACTIVE));

      // When & Then
      assertThatThrownBy(() -> service.execute(HOTEL_ID, ADMIN_ID))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.VALIDATION_FAILED));
      then(roomTypes).shouldHaveNoInteractions();
      then(history).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả HOTEL_NOT_FOUND khi khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelDoesNotExist() {
      // Given
      givenActiveAdmin();
      given(hotels.findByIdForUpdate(HOTEL_ID)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(HOTEL_ID, ADMIN_ID))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_FOUND));
    }

    @Test
    @DisplayName("Trả ACCESS_DENIED và không khóa hàng khi tài khoản không còn là ADMIN hợp lệ")
    void shouldThrowAccessDeniedWithoutLockingWhenActorIsNotActiveAdmin() {
      // Given
      given(accounts.findById(ADMIN_ID))
          .willReturn(Optional.of(new AccountSnapshot(ADMIN_ID, true, true, Set.of("CUSTOMER"))));

      // When & Then
      assertThatThrownBy(() -> service.execute(HOTEL_ID, ADMIN_ID))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.ACCESS_DENIED));
      then(hotels).shouldHaveNoInteractions();
    }
  }
}

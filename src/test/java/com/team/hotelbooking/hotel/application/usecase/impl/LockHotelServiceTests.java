package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.command.LockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ khóa bán khách sạn (HT-09)")
class LockHotelServiceTests extends HotelStatusServiceFixture {

  private LockHotelService service;

  @BeforeEach
  void setUp() {
    service = new LockHotelService(hotels, roomTypes, rooms, history, clock);
  }

  @Nested
  @DisplayName("LockHotelUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Khóa khách sạn ACTIVE và lưu lý do vào lịch sử")
    void shouldLockAndRecordReasonWhenHotelIsActive() {
      // Given
      givenLockedRow(hotel(Hotel.Status.ACTIVE));
      givenStatusUpdateReturnsSavedHotel();
      ArgumentCaptor<HotelStatusChange> change = ArgumentCaptor.forClass(HotelStatusChange.class);

      // When
      ManagementHotelResult result =
          service.execute(new LockHotelCommand(ADMIN_ID, HOTEL_ID, "Vi phạm quy định"));

      // Then
      assertThat(result.status()).isEqualTo("LOCKED");
      then(history).should().add(change.capture());
      assertThat(change.getValue().fromStatus()).isEqualTo(Hotel.Status.ACTIVE);
      assertThat(change.getValue().reason()).isEqualTo("Vi phạm quy định");
    }

    @Test
    @DisplayName("Không đổi trạng thái khi thiếu lý do (use case gọi ngoài HTTP)")
    void shouldRejectWithoutWritingWhenReasonIsBlank() {
      // Given
      givenLockedRow(hotel(Hotel.Status.ACTIVE));

      // When & Then
      assertThatThrownBy(() -> service.execute(new LockHotelCommand(ADMIN_ID, HOTEL_ID, " ")))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.VALIDATION_FAILED));
      then(hotels).should(never()).updateStatus(any());
      then(history).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả 409 khi khách sạn đã bị khóa")
    void shouldRejectWhenHotelIsAlreadyLocked() {
      // Given
      givenLockedRow(hotel(Hotel.Status.LOCKED));

      // When & Then
      assertThatThrownBy(
              () -> service.execute(new LockHotelCommand(ADMIN_ID, HOTEL_ID, "Khóa lần hai")))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
    }
  }
}

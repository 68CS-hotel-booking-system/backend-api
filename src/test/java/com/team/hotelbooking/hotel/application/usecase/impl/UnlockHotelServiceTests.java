package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team.hotelbooking.hotel.application.command.UnlockHotelCommand;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ gỡ khóa bán khách sạn (HT-09)")
class UnlockHotelServiceTests extends HotelStatusServiceFixture {

  private UnlockHotelService service;

  @BeforeEach
  void setUp() {
    service = new UnlockHotelService(hotels, roomTypes, rooms, history, clock);
  }

  private void givenLatestLockFrom(Hotel.Status statusBeforeLock) {
    given(history.findLatestInto(HOTEL_ID, Hotel.Status.LOCKED))
        .willReturn(
            Optional.of(
                new HotelStatusChange(
                    UUID.randomUUID(),
                    HOTEL_ID,
                    statusBeforeLock,
                    Hotel.Status.LOCKED,
                    "Khóa",
                    ADMIN_ID,
                    CREATED)));
  }

  private UnlockHotelCommand command() {
    return new UnlockHotelCommand(ADMIN_ID, HOTEL_ID, "Đã xử lý vi phạm");
  }

  @Nested
  @DisplayName("UnlockHotelUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Khóa từ ACTIVE thì gỡ về SUSPENDED, không mở bán ngay")
    void shouldUnlockToSuspendedWhenHotelWasActiveBeforeLock() {
      // Given
      givenLockedRow(hotel(Hotel.Status.LOCKED));
      givenLatestLockFrom(Hotel.Status.ACTIVE);
      givenStatusUpdateReturnsSavedHotel();

      // When & Then
      assertThat(service.execute(command()).status()).isEqualTo("SUSPENDED");
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Khóa khi còn DRAFT thì gỡ về DRAFT để quản lý không tự công khai")
    void shouldUnlockToDraftWhenHotelWasDraftBeforeLock() {
      // Given
      givenLockedRow(hotel(Hotel.Status.LOCKED));
      givenLatestLockFrom(Hotel.Status.DRAFT);
      givenStatusUpdateReturnsSavedHotel();

      // When & Then
      assertThat(service.execute(command()).status()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Thiếu lịch sử khóa (dữ liệu sửa tay) thì về DRAFT cho an toàn")
    void shouldUnlockToDraftWhenLockHistoryIsMissing() {
      // Given
      givenLockedRow(hotel(Hotel.Status.LOCKED));
      given(history.findLatestInto(HOTEL_ID, Hotel.Status.LOCKED)).willReturn(Optional.empty());
      givenStatusUpdateReturnsSavedHotel();

      // When & Then
      assertThat(service.execute(command()).status()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Trả 409 khi khách sạn không bị khóa")
    void shouldRejectWhenHotelIsNotLocked() {
      // Given
      givenLockedRow(hotel(Hotel.Status.SUSPENDED));
      given(history.findLatestInto(HOTEL_ID, Hotel.Status.LOCKED)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command()))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
    }
  }
}

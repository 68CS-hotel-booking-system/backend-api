package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.command.ResumeHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ mở bán lại khách sạn (HT-03, HT-09)")
class ResumeHotelServiceTests extends HotelStatusServiceFixture {

  private ResumeHotelService service;

  @BeforeEach
  void setUp() {
    service = new ResumeHotelService(hotels, roomTypes, rooms, history, clock);
  }

  private void givenOwnerSees(Hotel hotel) {
    given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
    givenLockedRow(hotel);
  }

  private ResumeHotelCommand ownerCommand() {
    return new ResumeHotelCommand(OWNER_ID, false, HOTEL_ID, null);
  }

  @Nested
  @DisplayName("ResumeHotelUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Owner mở lại khách sạn SUSPENDED khi vẫn đủ điều kiện HT-03")
    void shouldResumeAndRecordHistoryWhenHotelIsSuspendedAndReady() {
      // Given
      givenOwnerSees(hotel(Hotel.Status.SUSPENDED));
      givenSellableInventory(1);
      givenStatusUpdateReturnsSavedHotel();
      ArgumentCaptor<HotelStatusChange> change = ArgumentCaptor.forClass(HotelStatusChange.class);

      // When
      ManagementHotelResult result = service.execute(ownerCommand());

      // Then
      assertThat(result.status()).isEqualTo("ACTIVE");
      then(history).should().add(change.capture());
      assertThat(change.getValue().fromStatus()).isEqualTo(Hotel.Status.SUSPENDED);
      assertThat(change.getValue().toStatus()).isEqualTo(Hotel.Status.ACTIVE);
    }

    @Test
    @DisplayName("Trả 422 khi phòng vật lý đã ngừng phục vụ trong lúc tạm ngừng")
    void shouldRejectResumeWhenNoOperationalRoomRemains() {
      // Given
      givenOwnerSees(hotel(Hotel.Status.SUSPENDED));
      givenSellableInventory(0);

      // When & Then
      assertThatThrownBy(() -> service.execute(ownerCommand()))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_NOT_READY));
      then(hotels).should(never()).updateStatus(any());
    }

    @Test
    @DisplayName("Trả 409 khi quản lý cố mở bán khách sạn chưa công khai lần đầu")
    void shouldRejectWhenHotelIsStillDraft() {
      // Given
      givenOwnerSees(hotel(Hotel.Status.DRAFT));

      // When & Then
      assertThatThrownBy(() -> service.execute(ownerCommand()))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả HOTEL_LOCKED: quản lý không gỡ khóa của quản trị hệ thống")
    void shouldRejectWhenHotelIsLocked() {
      // Given
      givenOwnerSees(hotel(Hotel.Status.LOCKED));

      // When & Then
      assertThatThrownBy(() -> service.execute(ownerCommand()))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_LOCKED));
      then(history).shouldHaveNoInteractions();
    }
  }
}

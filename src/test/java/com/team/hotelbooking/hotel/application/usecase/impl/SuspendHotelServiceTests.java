package com.team.hotelbooking.hotel.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.application.command.SuspendHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.model.HotelStatusChange;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ tạm ngừng bán khách sạn (HT-05, HT-06, HT-09)")
class SuspendHotelServiceTests extends HotelStatusServiceFixture {

  private static final UUID MANAGER_ID = UUID.fromString("2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d");

  private SuspendHotelService service;

  @BeforeEach
  void setUp() {
    service = new SuspendHotelService(hotels, roomTypes, rooms, history, clock);
  }

  @Nested
  @DisplayName("SuspendHotelUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("HOTEL_MANAGER tạm ngừng khách sạn ACTIVE và ghi lịch sử, không kiểm HT-03")
    void shouldSuspendAndRecordHistoryWhenActorIsHotelManager() {
      // Given
      Hotel active = hotel(Hotel.Status.ACTIVE);
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(active));
      given(hotels.findActiveMemberRoles(HOTEL_ID, MANAGER_ID))
          .willReturn(Set.of(HotelMemberRole.HOTEL_MANAGER));
      givenLockedRow(active);
      givenStatusUpdateReturnsSavedHotel();
      ArgumentCaptor<HotelStatusChange> change = ArgumentCaptor.forClass(HotelStatusChange.class);

      // When
      ManagementHotelResult result =
          service.execute(new SuspendHotelCommand(MANAGER_ID, false, HOTEL_ID, "Sửa hồ bơi"));

      // Then
      assertThat(result.status()).isEqualTo("SUSPENDED");
      then(history).should().add(change.capture());
      assertThat(change.getValue().fromStatus()).isEqualTo(Hotel.Status.ACTIVE);
      assertThat(change.getValue().toStatus()).isEqualTo(Hotel.Status.SUSPENDED);
      assertThat(change.getValue().changedByUserId()).isEqualTo(MANAGER_ID);
      then(roomTypes).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả ACCESS_DENIED và không khóa hàng khi người gọi là RECEPTIONIST")
    void shouldThrowAccessDeniedWhenActorIsReceptionist() {
      // Given
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(hotel(Hotel.Status.ACTIVE)));
      given(hotels.findActiveMemberRoles(HOTEL_ID, MANAGER_ID))
          .willReturn(Set.of(HotelMemberRole.RECEPTIONIST));

      // When & Then
      assertThatThrownBy(
              () -> service.execute(new SuspendHotelCommand(MANAGER_ID, false, HOTEL_ID, null)))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.ACCESS_DENIED));
      then(hotels).should(never()).findByIdForUpdate(any());
      then(history).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả HOTEL_LOCKED khi quản lý tạm ngừng khách sạn bị khóa")
    void shouldThrowHotelLockedWhenHotelIsLocked() {
      // Given
      Hotel locked = hotel(Hotel.Status.LOCKED);
      given(hotels.findById(HOTEL_ID)).willReturn(Optional.of(locked));
      givenLockedRow(locked);

      // When & Then
      assertThatThrownBy(
              () -> service.execute(new SuspendHotelCommand(OWNER_ID, false, HOTEL_ID, null)))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_LOCKED));
      then(hotels).should(never()).updateStatus(any());
    }
  }
}

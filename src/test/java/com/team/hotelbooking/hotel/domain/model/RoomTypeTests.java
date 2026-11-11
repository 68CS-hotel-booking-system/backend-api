package com.team.hotelbooking.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cấu hình giới hạn sức chứa loại phòng")
class RoomTypeTests {

  private final Instant now = Instant.parse("2026-11-11T00:00:00Z");

  @Test
  @DisplayName("Loại phòng mới có tổng sức chứa không nhỏ hơn từng giới hạn thành phần")
  void shouldCreateConsistentCapacityWhenAdultsExceedDefaultOccupancy() {
    RoomType roomType =
        RoomType.create(
            UUID.randomUUID(), UUID.randomUUID(), "Family", null, (short) 4, (short) 3, now);

    assertThat(roomType.maxOccupancy()).isEqualTo((short) 4);
  }

  @Test
  @DisplayName("Cho phép tổng giới hạn người lớn và trẻ em lớn hơn tổng sức chứa")
  void shouldAllowCombinedLimitsAboveTotalCapacity() {
    RoomType roomType =
        RoomType.create(
            UUID.randomUUID(), UUID.randomUUID(), "Family", null, (short) 2, (short) 2, now);

    RoomType updated = roomType.update(null, null, null, null, (short) 3, null, null, now);

    assertThat(updated.maxAdults()).isEqualTo((short) 2);
    assertThat(updated.maxChildren()).isEqualTo((short) 2);
    assertThat(updated.maxOccupancy()).isEqualTo((short) 3);
  }

  @Test
  @DisplayName("Không cho phép giới hạn trẻ em vượt tổng sức chứa")
  void shouldRejectChildrenLimitAboveTotalCapacity() {
    RoomType roomType =
        RoomType.create(
            UUID.randomUUID(), UUID.randomUUID(), "Family", null, (short) 2, (short) 2, now);

    assertThatThrownBy(() -> roomType.update(null, null, null, (short) 3, null, null, null, now))
        .isInstanceOf(HotelException.class);
  }
}

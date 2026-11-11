package com.team.hotelbooking.hotel.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tiện ích riêng của khách sạn và phòng vật lý")
class CustomAmenityTests {
  private final UUID id = UUID.randomUUID();
  private final UUID hotelId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-11T10:00:00Z");

  @Test
  void shouldAttachAndNormalizeWhenCreated() {
    var item = CustomAmenity.create(id, hotelId, null, "  Hồ   Bơi  ", "  Nước khoáng  ", now);
    assertEquals("Hồ Bơi", item.name());
    assertEquals("hồ bơi", item.normalizedName());
    assertEquals("Nước khoáng", item.description());
    assertTrue(item.attached());
    assertEquals(0, item.version());
    assertEquals(now, item.createdAt());
  }

  @Test
  void shouldKeepIdentityAndOwnershipWhenEditingOrReattaching() {
    UUID roomId = UUID.randomUUID();
    var item = CustomAmenity.create(id, hotelId, roomId, "Ban công", "Hướng biển", now);
    var detached = item.update(null, null, false, now.plusSeconds(1));
    assertFalse(detached.attached());
    assertEquals(item.name(), detached.name());
    var updated = detached.update("Ban công riêng", "", true, now.plusSeconds(2));
    assertEquals(id, updated.id());
    assertEquals(hotelId, updated.hotelId());
    assertEquals(roomId, updated.roomId());
    assertEquals(now, updated.createdAt());
    assertNull(updated.description());
    assertTrue(updated.attached());
  }

  @Test
  void shouldRejectBlankOrOversizedNamesWhenCreated() {
    assertThrows(
        HotelException.class, () -> CustomAmenity.create(id, hotelId, null, "  ", null, now));
    assertThrows(
        HotelException.class,
        () -> CustomAmenity.create(id, hotelId, null, "x".repeat(101), null, now));
  }

  @Test
  void shouldRejectOversizedDescriptionWhenCreated() {
    assertThrows(
        HotelException.class,
        () -> CustomAmenity.create(id, hotelId, null, "Tiện ích", "x".repeat(2001), now));
  }

  @Test
  void shouldRejectEmptyPatchWhenNoFieldIsProvided() {
    var item = CustomAmenity.create(id, hotelId, null, "Tiện ích", null, now);
    assertThrows(HotelException.class, () -> item.update(null, null, null, now));
  }
}

package com.team.hotelbooking.inventory.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.hotel.RoomInventoryDirectory;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomAssignmentJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryRoomDirectoryAdapterTests {
  @Mock HotelCatalogPort hotels;
  @Mock InventoryQueryPort inventory;
  @Mock RoomAssignmentJpaRepository assignments;
  private InventoryRoomDirectoryAdapter adapter;
  private final UUID type = UUID.randomUUID();
  private final UUID room = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    adapter =
        new InventoryRoomDirectoryAdapter(
            hotels,
            inventory,
            assignments,
            Clock.fixed(Instant.parse("2026-11-11T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void shouldRejectWhenConfirmedDemandCannotBeReallocated() {
    given(inventory.canAllocateAfterAssignment(eq(type), any(), any())).willReturn(false);
    assertThrows(
        RoomInventoryDirectory.RoomChangeConflict.class,
        () -> adapter.requireAllocatableAfterDeactivation(type, room));
  }

  @Test
  void shouldAllowWhenRemainingRoomsCanServeAllBookings() {
    given(inventory.canAllocateAfterAssignment(eq(type), any(), any())).willReturn(true);
    assertDoesNotThrow(() -> adapter.requireAllocatableAfterDeactivation(type, room));
  }

  @Test
  void shouldRejectAssignedRoomEvenWhenOtherCapacityExists() {
    given(assignments.findActiveByRoomIdOverlapping(eq(room), any(), any()))
        .willReturn(List.of(new RoomAssignmentJpaEntity()));
    assertThrows(
        RoomInventoryDirectory.RoomChangeConflict.class,
        () -> adapter.requireAllocatableAfterDeactivation(type, room));
    then(inventory).should(never()).canAllocateAfterAssignment(any(), any(), any());
  }
}

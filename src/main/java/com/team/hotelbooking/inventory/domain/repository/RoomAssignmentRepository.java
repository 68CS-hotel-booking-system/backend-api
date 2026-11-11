package com.team.hotelbooking.inventory.domain.repository;

import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoomAssignmentRepository {

  RoomAssignment save(RoomAssignment assignment);

  List<RoomAssignment> saveAll(List<RoomAssignment> assignments);

  List<RoomAssignment> findActiveByBookingId(UUID bookingId);

  List<RoomAssignment> findAllByBookingId(UUID bookingId);

  List<RoomAssignment> findActiveByRoomIdOverlapping(
      UUID roomId, LocalDate startDate, LocalDate endDate);

  List<RoomAssignment> findActiveByHotelIdOverlapping(
      UUID hotelId, LocalDate startDate, LocalDate endDate);

  List<RoomAssignment> findActiveByRoomTypeIdsOverlapping(
      Collection<UUID> roomTypeIds, LocalDate startDate, LocalDate endDate);
}

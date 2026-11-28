package com.team.hotelbooking.hotel.application.port;

import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelMemberStore {

  PageResult<MemberRecord> pageByHotel(UUID hotelId, PageQuery pagination);

  Optional<MemberRecord> findById(UUID hotelId, UUID memberId);

  Optional<MemberRecord> findByHotelUserRole(UUID hotelId, UUID userId, HotelMemberRole role);

  boolean hasOtherActiveMembership(UUID hotelId, UUID userId, UUID excludeMemberId);

  MemberRecord insert(UUID hotelId, UUID userId, HotelMemberRole role, UUID actorId, Instant now);

  int updateStatus(UUID memberId, boolean active, long expectedVersion, UUID actorId, Instant now);

  PageResult<StaffHotelRecord> pageStaffHotels(UUID actorId, PageQuery pagination);

  List<WorkplaceRecord> findWorkplaces(UUID userId);

  boolean hasAnyActiveMembership(UUID userId);

  record MemberRecord(
      UUID id,
      UUID hotelId,
      UUID userId,
      String fullName,
      String email,
      HotelMemberRole role,
      boolean active,
      Instant assignedAt,
      Instant updatedAt,
      long version) {}

  record StaffHotelRecord(UUID id, String name, boolean canAssignManager) {}

  record WorkplaceRecord(UUID hotelId, String hotelName, HotelMemberRole memberRole) {}
}

package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore.MemberRecord;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.StaffHotelRecord;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.WorkplaceRecord;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.List;
import java.util.UUID;

public interface HotelStaffUseCase {

  PageResult<StaffHotelRecord> listStaffHotels(UUID actorId, PageQuery pagination);

  PageResult<MemberRecord> listMembers(UUID actorId, UUID hotelId, PageQuery pagination);

  MemberRecord addMember(UUID actorId, UUID hotelId, String email, HotelMemberRole memberRole);

  MemberRecord updateMemberStatus(
      UUID actorId, UUID hotelId, UUID memberId, String status, long expectedVersion);

  List<WorkplaceRecord> listWorkplaces(UUID actorId);
}

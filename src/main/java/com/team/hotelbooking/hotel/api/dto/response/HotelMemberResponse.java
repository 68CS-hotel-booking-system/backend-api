package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore.MemberRecord;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.util.UUID;

public record HotelMemberResponse(
    UUID id,
    UUID userId,
    String fullName,
    String email,
    HotelMemberRole memberRole,
    String status,
    Instant assignedAt,
    Instant updatedAt,
    long version) {

  public static HotelMemberResponse from(MemberRecord record) {
    return new HotelMemberResponse(
        record.id(),
        record.userId(),
        record.fullName(),
        record.email(),
        record.role(),
        record.active() ? "ACTIVE" : "REVOKED",
        record.assignedAt(),
        record.updatedAt(),
        record.version());
  }

  public static PageResponse<HotelMemberResponse> from(PageResult<MemberRecord> result) {
    return PageResponse.from(result.map(HotelMemberResponse::from));
  }
}

package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.MemberRecord;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.StaffHotelRecord;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.WorkplaceRecord;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.usecase.HotelStaffUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HotelStaffService implements HotelStaffUseCase {

  private final HotelRepository hotels;
  private final HotelMemberStore members;
  private final IdentityAccounts accounts;
  private final HotelManagementAccess access;
  private final Clock clock;
  private final int maxPageSize;

  public HotelStaffService(
      HotelRepository hotels,
      HotelMemberStore members,
      IdentityAccounts accounts,
      Clock clock,
      int maxPageSize) {
    if (maxPageSize < 1) {
      throw new IllegalArgumentException("Maximum page size must be positive.");
    }
    this.hotels = hotels;
    this.members = members;
    this.accounts = accounts;
    this.access = new HotelManagementAccess(hotels);
    this.clock = clock;
    this.maxPageSize = maxPageSize;
  }

  @Override
  public PageResult<StaffHotelRecord> listStaffHotels(UUID actorId, PageQuery pagination) {
    validatePagination(pagination);
    AccountSnapshot actor = requireNonAdminActor(actorId);
    return members.pageStaffHotels(actor.id(), pagination);
  }

  @Override
  public PageResult<MemberRecord> listMembers(UUID actorId, UUID hotelId, PageQuery pagination) {
    if (hotelId == null) {
      throw HotelException.invalid("Mã khách sạn là bắt buộc.");
    }
    validatePagination(pagination);
    AccountSnapshot actor = requireNonAdminActor(actorId);

    Hotel hotel =
        hotels
            .findById(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
    requireStaffManagementLevel(actor.id(), hotel);
    return members.pageByHotel(hotel.id(), pagination);
  }

  @Override
  public MemberRecord addMember(
      UUID actorId, UUID hotelId, String email, HotelMemberRole memberRole) {
    if (hotelId == null || memberRole == null || email == null || email.isBlank()) {
      throw HotelException.invalid("Thông tin khách sạn, email và vai trò nhân sự là bắt buộc.");
    }
    AccountSnapshot actor = requireNonAdminActor(actorId);

    Hotel hotel =
        hotels
            .findByIdForUpdate(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
    HotelManagementAccess.Level level = requireStaffManagementLevel(actor.id(), hotel);
    if (level == HotelManagementAccess.Level.MANAGER
        && memberRole == HotelMemberRole.HOTEL_MANAGER) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    AccountSnapshot target =
        accounts
            .findByEmail(email)
            .filter(account -> account.active() && account.emailVerified())
            .orElseThrow(
                () ->
                    HotelException.fieldError(
                        HotelErrorCode.STAFF_ACCOUNT_NOT_ELIGIBLE,
                        "email",
                        HotelErrorCode.STAFF_ACCOUNT_NOT_ELIGIBLE.defaultMessage()));

    if (target.id().equals(hotel.ownerId())) {
      throw HotelException.fieldError(
          HotelErrorCode.STAFF_ACCOUNT_NOT_ELIGIBLE,
          "email",
          "Tài khoản này là chủ sở hữu của khách sạn.");
    }
    if (target.id().equals(actor.id())) {
      throw HotelException.fieldError(
          HotelErrorCode.STAFF_ACCOUNT_NOT_ELIGIBLE,
          "email",
          "Không thể tự phân công chính tài khoản của bạn.");
    }
    if (target.roles().contains("ADMIN")) {
      throw HotelException.fieldError(
          HotelErrorCode.STAFF_ACCOUNT_NOT_ELIGIBLE,
          "email",
          "Không thể phân công tài khoản quản trị hệ thống làm nhân sự khách sạn.");
    }

    if (members.hasOtherActiveMembership(hotel.id(), target.id(), null)) {
      throw new HotelException(HotelErrorCode.HOTEL_MEMBER_ALREADY_EXISTS);
    }

    Optional<MemberRecord> existing =
        members.findByHotelUserRole(hotel.id(), target.id(), memberRole);
    if (existing.isPresent()) {
      MemberRecord record = existing.get();
      int updated =
          members.updateStatus(
              record.id(), true, record.version(), actor.id(), clock.instant());
      if (updated == 0) {
        throw new HotelException(HotelErrorCode.HOTEL_MEMBER_VERSION_CONFLICT);
      }
      return members
          .findById(hotel.id(), record.id())
          .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_MEMBER_NOT_FOUND));
    }

    return members.insert(hotel.id(), target.id(), memberRole, actor.id(), clock.instant());
  }

  @Override
  public MemberRecord updateMemberStatus(
      UUID actorId, UUID hotelId, UUID memberId, String status, long expectedVersion) {
    if (hotelId == null || memberId == null || status == null || expectedVersion < 0) {
      throw HotelException.invalid("Thông tin cập nhật trạng thái nhân sự không hợp lệ.");
    }
    boolean targetActive;
    if ("ACTIVE".equals(status)) {
      targetActive = true;
    } else if ("REVOKED".equals(status)) {
      targetActive = false;
    } else {
      throw HotelException.invalid("Trạng thái phân công phải là ACTIVE hoặc REVOKED.");
    }

    AccountSnapshot actor = requireNonAdminActor(actorId);
    Hotel hotel =
        hotels
            .findByIdForUpdate(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
    HotelManagementAccess.Level level = requireStaffManagementLevel(actor.id(), hotel);

    MemberRecord member =
        members
            .findById(hotel.id(), memberId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_MEMBER_NOT_FOUND));

    if (level == HotelManagementAccess.Level.MANAGER
        && (member.role() == HotelMemberRole.HOTEL_MANAGER
            || member.userId().equals(actor.id()))) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    if (targetActive
        && members.hasOtherActiveMembership(hotel.id(), member.userId(), member.id())) {
      throw new HotelException(HotelErrorCode.HOTEL_MEMBER_ALREADY_EXISTS);
    }

    int updated =
        members.updateStatus(
            member.id(), targetActive, expectedVersion, actor.id(), clock.instant());
    if (updated == 0) {
      throw new HotelException(HotelErrorCode.HOTEL_MEMBER_VERSION_CONFLICT);
    }

    return members
        .findById(hotel.id(), member.id())
        .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_MEMBER_NOT_FOUND));
  }

  @Override
  public List<WorkplaceRecord> listWorkplaces(UUID actorId) {
    AccountSnapshot actor = requireActiveVerifiedActor(actorId);
    return members.findWorkplaces(actor.id());
  }

  private AccountSnapshot requireActiveVerifiedActor(UUID actorId) {
    if (actorId == null) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    AccountSnapshot actor =
        accounts
            .findById(actorId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));
    if (!actor.active() || !actor.emailVerified()) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    return actor;
  }

  private AccountSnapshot requireNonAdminActor(UUID actorId) {
    AccountSnapshot actor = requireActiveVerifiedActor(actorId);
    if (actor.roles().contains("ADMIN")) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    return actor;
  }

  private HotelManagementAccess.Level requireStaffManagementLevel(UUID actorId, Hotel hotel) {
    HotelManagementAccess.Level level = access.levelOf(actorId, false, hotel);
    return switch (level) {
      case OWNER, MANAGER -> level;
      case RECEPTIONIST, ADMIN -> throw new HotelException(HotelErrorCode.ACCESS_DENIED);
      case NONE -> throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    };
  }

  private void validatePagination(PageQuery pagination) {
    if (pagination == null
        || pagination.page() < 0
        || pagination.size() < 1
        || pagination.size() > maxPageSize) {
      throw HotelException.invalid(
          "Số trang không được âm và kích thước trang phải từ 1 đến " + maxPageSize + ".");
    }
    if ((long) pagination.page() * pagination.size() > Integer.MAX_VALUE) {
      throw HotelException.invalid("Trang yêu cầu vượt quá phạm vi phân trang được hỗ trợ.");
    }
  }
}

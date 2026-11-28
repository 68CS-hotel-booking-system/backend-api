package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ListManagementHotelsQuery;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;

public class ListManagementHotelsService implements ListManagementHotelsUseCase {

  private final HotelRepository hotels;
  private final IdentityAccounts accounts;
  private final HotelMemberStore members;
  private final HotelManagementAccess access;
  private final int maxPageSize;

  public ListManagementHotelsService(
      HotelRepository hotels, IdentityAccounts accounts, int maxPageSize) {
    this(hotels, accounts, null, maxPageSize);
  }

  public ListManagementHotelsService(
      HotelRepository hotels,
      IdentityAccounts accounts,
      HotelMemberStore members,
      int maxPageSize) {
    if (maxPageSize < 1) {
      throw new IllegalArgumentException("Maximum page size must be positive.");
    }
    this.hotels = hotels;
    this.accounts = accounts;
    this.members = members;
    this.access = new HotelManagementAccess(hotels);
    this.maxPageSize = maxPageSize;
  }

  @Override
  public PageResult<ManagementHotelResult> execute(ListManagementHotelsQuery query) {
    if (query == null || query.actorId() == null || query.pagination() == null) {
      throw HotelException.invalid("Thông tin người thực hiện và phân trang là bắt buộc.");
    }

    PageQuery pagination = query.pagination();
    if (pagination.page() < 0 || pagination.size() < 1 || pagination.size() > maxPageSize) {
      throw HotelException.invalid(
          "Số trang không được âm và kích thước trang phải từ 1 đến " + maxPageSize + ".");
    }
    if ((long) pagination.page() * pagination.size() > Integer.MAX_VALUE) {
      throw HotelException.invalid("Trang yêu cầu vượt quá phạm vi phân trang được hỗ trợ.");
    }

    AccountSnapshot actor =
        accounts
            .findById(query.actorId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));

    boolean admin = actor.roles().contains("ADMIN");
    boolean ownerRole = actor.roles().contains("HOTEL_OWNER");
    boolean hasStaff =
        !admin && !ownerRole && members != null && members.hasAnyActiveMembership(actor.id());
    if (!actor.active() || !actor.emailVerified() || (!admin && !ownerRole && !hasStaff)) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }

    PageResult<Hotel> result =
        admin ? hotels.findAll(pagination) : hotels.findManagedByUser(actor.id(), pagination);

    return result.map(
        hotel ->
            ManagementHotelResult.from(hotel, access.levelOf(actor.id(), admin, hotel).name()));
  }
}

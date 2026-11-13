package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ApproveHotelReviewCommand;
import com.team.hotelbooking.hotel.application.command.GetHotelReviewQuery;
import com.team.hotelbooking.hotel.application.command.ListHotelReviewsQuery;
import com.team.hotelbooking.hotel.application.command.ListManagementHotelReviewsQuery;
import com.team.hotelbooking.hotel.application.command.RejectHotelReviewCommand;
import com.team.hotelbooking.hotel.application.command.SubmitHotelReviewCommand;
import com.team.hotelbooking.hotel.application.port.HotelReviewSnapshotPort;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.result.HotelReviewResult;
import com.team.hotelbooking.hotel.application.result.HotelReviewSummaryResult;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListHotelReviewsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelReviewsUseCase;
import com.team.hotelbooking.hotel.application.usecase.RejectHotelReviewUseCase;
import com.team.hotelbooking.hotel.application.usecase.SubmitHotelReviewUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelReviewRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Gửi, sửa và quyết định đều khóa hotel trước; quyết định sau đó mới cập nhật hồ sơ. */
public final class HotelReviewService
    implements SubmitHotelReviewUseCase,
        GetHotelReviewUseCase,
        ListHotelReviewsUseCase,
        ApproveHotelReviewUseCase,
        RejectHotelReviewUseCase,
        ListManagementHotelReviewsUseCase {
  private final HotelRepository hotels;
  private final HotelReviewRepository reviews;
  private final HotelReviewSnapshotPort snapshots;
  private final IdentityAccounts accounts;
  private final HotelSalesLifecycle lifecycle;
  private final Clock clock;
  private final int maxPageSize;

  public HotelReviewService(
      HotelRepository hotels,
      HotelReviewRepository reviews,
      HotelReviewSnapshotPort snapshots,
      IdentityAccounts accounts,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository history,
      Clock clock,
      int maxPageSize) {
    this.hotels = hotels;
    this.reviews = reviews;
    this.snapshots = snapshots;
    this.accounts = accounts;
    this.lifecycle = new HotelSalesLifecycle(hotels, roomTypes, rooms, history);
    this.clock = clock;
    this.maxPageSize = maxPageSize;
  }

  @Override
  public HotelReviewResult execute(SubmitHotelReviewCommand command) {
    if (command == null) throw HotelException.invalid("Thông tin hồ sơ là bắt buộc.");
    requireIdsAndVersion(command.actorId(), command.hotelId(), command.expectedVersion());
    var owner = requireAccount(command.actorId());
    // Admin kể cả có đồng thời role Owner cũng không gửi hoặc sửa hồ sơ thay Owner.
    if (!owner.roles().contains("HOTEL_OWNER") || owner.roles().contains("ADMIN")) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    Hotel hotel = lifecycle.lockForStatusChange(command.hotelId());
    if (!hotel.ownerId().equals(owner.id())) {
      throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    }
    requireDraft(hotel);
    if (hotel.version() != command.expectedVersion()) {
      throw new HotelException(HotelErrorCode.REVIEW_CONCURRENT_UPDATE);
    }
    if (reviews.findPendingByHotelId(hotel.id()).isPresent()) {
      throw new HotelException(HotelErrorCode.REVIEW_PENDING);
    }
    Instant now = clock.instant();
    lifecycle.requireReadyToSell(hotel, now);
    return HotelReviewResult.from(
        reviews.add(HotelReviewRequest.submit(snapshots.capture(hotel), owner.id(), now)));
  }

  @Override
  public HotelReviewResult execute(GetHotelReviewQuery query) {
    if (query == null || query.requestId() == null) {
      throw HotelException.invalid("ID hồ sơ là bắt buộc.");
    }
    var actor = requireAccount(query.actorId());
    HotelReviewRequest review = requireReview(query.requestId());
    if (!actor.roles().contains("ADMIN")) {
      Hotel hotel =
          hotels
              .findById(review.hotelId())
              .orElseThrow(() -> new HotelException(HotelErrorCode.REVIEW_NOT_FOUND));
      if (!actor.roles().contains("HOTEL_OWNER") || !hotel.ownerId().equals(actor.id())) {
        throw new HotelException(HotelErrorCode.REVIEW_NOT_FOUND);
      }
    }
    return HotelReviewResult.from(review);
  }

  @Override
  public PageResult<HotelReviewSummaryResult> execute(ListHotelReviewsQuery query) {
    if (query == null) throw HotelException.invalid("Thông tin truy vấn là bắt buộc.");
    requireAdmin(query.actorId());
    PageQuery page = query.pagination();
    requirePagination(page);
    return reviews
        .findAll(query.status(), query.hotelId(), page)
        .map(HotelReviewSummaryResult::from);
  }

  @Override
  public PageResult<HotelReviewSummaryResult> execute(ListManagementHotelReviewsQuery query) {
    if (query == null || query.hotelId() == null) {
      throw HotelException.invalid("ID khách sạn là bắt buộc.");
    }
    var actor = requireAccount(query.actorId());
    Hotel hotel =
        hotels
            .findById(query.hotelId())
            .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));
    if (!actor.roles().contains("ADMIN")
        && (!actor.roles().contains("HOTEL_OWNER") || !hotel.ownerId().equals(actor.id()))) {
      throw new HotelException(HotelErrorCode.HOTEL_NOT_FOUND);
    }
    requirePagination(query.pagination());
    return reviews
        .findAll(null, hotel.id(), query.pagination())
        .map(HotelReviewSummaryResult::from);
  }

  private void requirePagination(PageQuery page) {
    if (page == null
        || page.page() < 0
        || page.size() < 1
        || page.size() > maxPageSize
        || (long) page.page() * page.size() > Integer.MAX_VALUE) {
      throw HotelException.invalid("Thông tin phân trang không hợp lệ.");
    }
  }

  @Override
  public HotelReviewResult execute(ApproveHotelReviewCommand command) {
    if (command == null) throw HotelException.invalid("Thông tin quyết định là bắt buộc.");
    requireIdsAndVersion(command.actorId(), command.requestId(), command.expectedVersion());
    requireAdmin(command.actorId());
    Hotel hotel = lockHotelOfReview(command.requestId());
    HotelReviewRequest review = requireReview(command.requestId());
    return HotelReviewResult.from(
        approveLocked(hotel, review, command.actorId(), command.expectedVersion()));
  }

  @Override
  public HotelReviewResult execute(RejectHotelReviewCommand command) {
    if (command == null) throw HotelException.invalid("Thông tin quyết định là bắt buộc.");
    requireIdsAndVersion(command.actorId(), command.requestId(), command.expectedVersion());
    requireAdmin(command.actorId());
    Hotel hotel = lockHotelOfReview(command.requestId());
    HotelReviewRequest review = requireReview(command.requestId());
    HotelReviewRequest decided =
        review.decide(
            HotelReviewRequest.Status.REJECTED,
            command.actorId(),
            clock.instant(),
            command.reason(),
            command.expectedVersion());
    requireDraft(hotel);
    return HotelReviewResult.from(reviews.updateDecision(decided));
  }

  // Adapter cho PATCH cũ: chỉ xử lý hồ sơ PENDING hiện tại sau khi đã khóa hotel.
  public ManagementHotelResult approvePendingForHotel(UUID hotelId, UUID adminId) {
    if (hotelId == null) throw HotelException.invalid("ID khách sạn là bắt buộc.");
    requireAdmin(adminId);
    Hotel hotel = lifecycle.lockForStatusChange(hotelId);
    HotelReviewRequest review =
        reviews
            .findPendingByHotelId(hotelId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.REVIEW_REQUIRED));
    approveLocked(hotel, review, adminId, review.version());
    return ManagementHotelResult.from(hotels.findById(hotelId).orElseThrow());
  }

  private HotelReviewRequest approveLocked(
      Hotel hotel, HotelReviewRequest review, UUID adminId, long expectedVersion) {
    Instant now = clock.instant();
    HotelReviewRequest decided =
        review.decide(HotelReviewRequest.Status.APPROVED, adminId, now, null, expectedVersion);
    requireDraft(hotel);
    Hotel activated = hotel.activate(now);
    lifecycle.requireReadyToSell(activated, now);
    lifecycle.record(hotel, activated, null, false, adminId);
    return reviews.updateDecision(decided);
  }

  private Hotel lockHotelOfReview(UUID requestId) {
    // Chỉ đọc ID trước khi khóa; không nạp entity hồ sơ cũ vào persistence context.
    UUID hotelId =
        reviews
            .findHotelId(requestId)
            .orElseThrow(() -> new HotelException(HotelErrorCode.REVIEW_NOT_FOUND));
    return lifecycle.lockForStatusChange(hotelId);
  }

  private HotelReviewRequest requireReview(UUID id) {
    return reviews
        .findById(id)
        .orElseThrow(() -> new HotelException(HotelErrorCode.REVIEW_NOT_FOUND));
  }

  private IdentityAccounts.AccountSnapshot requireAccount(UUID id) {
    if (id == null) throw HotelException.invalid("Người thực hiện là bắt buộc.");
    var actor =
        accounts.findById(id).orElseThrow(() -> new HotelException(HotelErrorCode.ACCESS_DENIED));
    if (!actor.active() || !actor.emailVerified()) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
    return actor;
  }

  private void requireAdmin(UUID id) {
    if (!requireAccount(id).roles().contains("ADMIN")) {
      throw new HotelException(HotelErrorCode.ACCESS_DENIED);
    }
  }

  private static void requireDraft(Hotel hotel) {
    if (hotel.status() != Hotel.Status.DRAFT) {
      throw new HotelException(HotelErrorCode.INVALID_STATUS_TRANSITION);
    }
  }

  private static void requireIdsAndVersion(UUID actorId, UUID resourceId, Long version) {
    if (actorId == null || resourceId == null || version == null || version < 0) {
      throw HotelException.invalid("Người thực hiện, ID và expectedVersion không âm là bắt buộc.");
    }
  }
}

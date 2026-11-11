package com.team.hotelbooking.booking.infrastructure.persistence.adapter;

import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Booking.BookingItem;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingItemJpaEntity;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingJpaEntity;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingNightPriceJpaEntity;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingStatusHistoryJpaEntity;
import com.team.hotelbooking.booking.infrastructure.persistence.mapper.BookingPersistenceMapper;
import com.team.hotelbooking.booking.infrastructure.persistence.repository.BookingItemJpaRepository;
import com.team.hotelbooking.booking.infrastructure.persistence.repository.BookingJpaRepository;
import com.team.hotelbooking.booking.infrastructure.persistence.repository.BookingNightPriceJpaRepository;
import com.team.hotelbooking.booking.infrastructure.persistence.repository.BookingStatusHistoryJpaRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaBookingRepositoryAdapter implements BookingRepository {

  private static final Sort DEFAULT_SORT =
      Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final BookingJpaRepository bookingJpaRepository;
  private final BookingItemJpaRepository bookingItemJpaRepository;
  private final BookingNightPriceJpaRepository bookingNightPriceJpaRepository;
  private final BookingStatusHistoryJpaRepository bookingStatusHistoryJpaRepository;
  private final BookingPersistenceMapper mapper;

  @Override
  public Booking add(Booking booking) {
    BookingJpaEntity newBooking = mapper.toBookingEntity(booking);
    // UUID đã được cấp ở domain; version null đánh dấu đây là bản ghi mới cho Spring Data JPA.
    newBooking.setVersion(null);
    BookingJpaEntity savedBooking = bookingJpaRepository.saveAndFlush(newBooking);

    List<BookingItemJpaEntity> itemEntities = new ArrayList<>();
    List<BookingNightPriceJpaEntity> nightEntities = new ArrayList<>();
    for (BookingItem item : booking.items()) {
      itemEntities.add(mapper.toItemEntity(booking, item));
      nightEntities.addAll(mapper.toNightPriceEntities(item));
    }

    List<BookingItemJpaEntity> savedItems = bookingItemJpaRepository.saveAllAndFlush(itemEntities);
    List<BookingNightPriceJpaEntity> savedNights =
        bookingNightPriceJpaRepository.saveAllAndFlush(nightEntities);

    BookingStatusHistoryJpaEntity history = new BookingStatusHistoryJpaEntity();
    history.setId(UUID.randomUUID());
    history.setBookingId(savedBooking.getId());
    history.setFromStatus(null);
    history.setToStatus(savedBooking.getStatus());
    history.setChangedByUserId(savedBooking.getCustomerId());
    history.setReason("Tạo đơn đặt phòng mới.");
    history.setCreatedAt(savedBooking.getCreatedAt());
    bookingStatusHistoryJpaRepository.saveAndFlush(history);

    Map<UUID, List<BookingNightPriceJpaEntity>> nightlyByItemId =
        savedNights.stream().collect(Collectors.groupingBy(n -> n.getId().getBookingItemId()));

    return mapper.toDomain(savedBooking, savedItems, nightlyByItemId);
  }

  @Override
  public Optional<Booking> findById(UUID bookingId) {
    return bookingJpaRepository.findById(bookingId).map(this::loadFullBooking);
  }

  @Override
  public Optional<Booking> findByIdAndCustomerId(UUID bookingId, UUID customerId) {
    return bookingJpaRepository
        .findByIdAndCustomerId(bookingId, customerId)
        .map(this::loadFullBooking);
  }

  @Override
  public Optional<Booking> findByIdAndHotelId(UUID bookingId, UUID hotelId) {
    return bookingJpaRepository.findByIdAndHotelId(bookingId, hotelId).map(this::loadFullBooking);
  }

  @Override
  public PageResult<Booking> findByCustomerId(
      UUID customerId, Booking.Status status, PageQuery pagination) {
    Pageable pageable = PageRequest.of(pagination.page(), pagination.size(), DEFAULT_SORT);
    Page<BookingJpaEntity> page =
        status == null
            ? bookingJpaRepository.findByCustomerId(customerId, pageable)
            : bookingJpaRepository.findByCustomerIdAndStatus(
                customerId, BookingJpaEntity.DbStatus.valueOf(status.name()), pageable);

    return PageResultMapper.from(page, entity -> mapper.toDomain(entity, List.of(), Map.of()));
  }

  @Override
  public PageResult<Booking> findByHotelId(
      UUID hotelId, Booking.Status status, PageQuery pagination) {
    Pageable pageable = PageRequest.of(pagination.page(), pagination.size(), DEFAULT_SORT);
    Page<BookingJpaEntity> page =
        status == null
            ? bookingJpaRepository.findByHotelId(hotelId, pageable)
            : bookingJpaRepository.findByHotelIdAndStatus(
                hotelId, BookingJpaEntity.DbStatus.valueOf(status.name()), pageable);

    return PageResultMapper.from(page, entity -> mapper.toDomain(entity, List.of(), Map.of()));
  }

  private Booking loadFullBooking(BookingJpaEntity entity) {
    List<BookingItemJpaEntity> items =
        bookingItemJpaRepository.findAllByBookingIdOrderByIdAsc(entity.getId());
    if (items.isEmpty()) {
      return mapper.toDomain(entity, List.of(), Map.of());
    }
    List<UUID> itemIds = items.stream().map(BookingItemJpaEntity::getId).toList();
    List<BookingNightPriceJpaEntity> nights =
        bookingNightPriceJpaRepository.findAllByIdBookingItemIdInOrderByIdStayDateAsc(itemIds);
    Map<UUID, List<BookingNightPriceJpaEntity>> nightlyByItemId =
        nights.stream().collect(Collectors.groupingBy(n -> n.getId().getBookingItemId()));
    return mapper.toDomain(entity, items, nightlyByItemId);
  }

  @Override
  public boolean updateStatus(UUID bookingId, Booking.Status status, Instant updatedAt) {
    Optional<BookingJpaEntity> entityOpt = bookingJpaRepository.findById(bookingId);
    if (entityOpt.isEmpty()) {
      return false;
    }
    BookingJpaEntity entity = entityOpt.get();
    BookingJpaEntity.DbStatus fromStatus = entity.getStatus();
    BookingJpaEntity.DbStatus toStatus = BookingJpaEntity.DbStatus.valueOf(status.name());
    if (fromStatus == toStatus) {
      return true;
    }
    boolean allowed =
        (toStatus == BookingJpaEntity.DbStatus.CHECKED_IN
                && fromStatus == BookingJpaEntity.DbStatus.CONFIRMED)
            || (toStatus == BookingJpaEntity.DbStatus.CHECKED_OUT
                && fromStatus == BookingJpaEntity.DbStatus.CHECKED_IN)
            || (toStatus == BookingJpaEntity.DbStatus.NO_SHOW
                && fromStatus == BookingJpaEntity.DbStatus.CONFIRMED);
    if (!allowed) {
      return false;
    }
    entity.setStatus(toStatus);
    entity.setUpdatedAt(updatedAt);
    bookingJpaRepository.saveAndFlush(entity);

    BookingStatusHistoryJpaEntity history = new BookingStatusHistoryJpaEntity();
    history.setId(UUID.randomUUID());
    history.setBookingId(bookingId);
    history.setFromStatus(fromStatus);
    history.setToStatus(toStatus);
    history.setChangedByUserId(null);
    history.setReason("Cập nhật trạng thái lưu trú: " + status.name());
    history.setCreatedAt(updatedAt != null ? updatedAt : Instant.now());
    bookingStatusHistoryJpaRepository.saveAndFlush(history);
    return true;
  }
}

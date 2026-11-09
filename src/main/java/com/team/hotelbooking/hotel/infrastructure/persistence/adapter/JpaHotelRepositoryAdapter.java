package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.HotelPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelSpecs;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.persistence.PageResultMapper;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaHotelRepositoryAdapter implements HotelRepository {

  private static final Sort MANAGEMENT_SORT =
    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private static final Sort PUBLIC_SORT =
    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final HotelJpaRepository hotelJpaRepository;

  @Override
  public Hotel add(Hotel hotel) {
    var entity = HotelPersistenceMapper.toNewEntity(hotel);

    try {
      var saved = hotelJpaRepository.saveAndFlush(entity);
      return HotelPersistenceMapper.toDomain(saved);
    } catch (DataIntegrityViolationException exception) {
      throw translateConflict(exception);
    }
  }

  @Override
  public Hotel save(Hotel hotel) {
    var entity =
      hotelJpaRepository
        .findById(hotel.id())
        .orElseThrow(() -> new HotelException(HotelErrorCode.HOTEL_NOT_FOUND));

    entity.setStatus(HotelPersistenceMapper.toDbStatus(hotel.status()));
    entity.setUpdatedAt(hotel.updatedAt());
    try {
      var saved = hotelJpaRepository.saveAndFlush(entity);
      return HotelPersistenceMapper.toDomain(saved);
    } catch (DataIntegrityViolationException exception) {
      throw translateConflict(exception);
    }
  }

  @Override
  public Optional<Hotel> findById(UUID hotelId) {
    return hotelJpaRepository.findById(hotelId).map(HotelPersistenceMapper::toDomain);
  }

  @Override
  public Optional<Hotel> findByIdAndOwnerId(UUID hotelId, UUID ownerId) {
    return hotelJpaRepository
      .findByIdAndOwnerId(hotelId, ownerId)
      .map(HotelPersistenceMapper::toDomain);
  }

  @Override
  public Optional<Hotel> findByIdAndStatus(UUID hotelId, Hotel.Status status) {
    return hotelJpaRepository
      .findByIdAndStatus(hotelId, HotelPersistenceMapper.toDbStatus(status))
      .map(HotelPersistenceMapper::toDomain);
  }

  @Override
  public PageResult<Hotel> findAll(PageQuery pagination) {
    return PageResultMapper.from(
      hotelJpaRepository.findAll(pageable(pagination)), HotelPersistenceMapper::toDomain);
  }

  @Override
  public PageResult<Hotel> findByOwnerId(UUID ownerId, PageQuery pagination) {
    return PageResultMapper.from(
      hotelJpaRepository.findByOwnerId(ownerId, pageable(pagination)),
      HotelPersistenceMapper::toDomain);
  }

  @Override
  public PageResult<Hotel> findPublicHotels(UUID cityId, String keyword, PageQuery pagination) {
    Pageable pageable = PageRequest.of(pagination.page(), pagination.size(), PUBLIC_SORT);

    return PageResultMapper.from(
      hotelJpaRepository.findAll(HotelSpecs.publicSearch(cityId, keyword), pageable),
      HotelPersistenceMapper::toDomain);
  }

  private static Pageable pageable(PageQuery pagination) {
    return PageRequest.of(pagination.page(), pagination.size(), MANAGEMENT_SORT);
  }

  private RuntimeException translateConflict(
    DataIntegrityViolationException exception) {

    for (Throwable cause = exception;
         cause != null;
         cause = cause.getCause()) {

      if (!(cause instanceof ConstraintViolationException violation)) {
        continue;
      }

      String sqlState = violation.getSQLException().getSQLState();
      String constraint = violation.getConstraintName();

      if ("23505".equals(sqlState)
        && "uq_hotels_slug".equals(constraint)) {
        return new HotelException(HotelErrorCode.HOTEL_SLUG_CONFLICT);
      }

      if ("23503".equals(sqlState)) {
        if ("fk_hotels_owner".equals(constraint)) {
          return new HotelException(HotelErrorCode.OWNER_NOT_FOUND);
        }

        if ("fk_hotels_city".equals(constraint)) {
          return new HotelException(HotelErrorCode.CITY_NOT_FOUND);
        }
      }
    }

    return exception;
  }
}

package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.HotelPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
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

  private static Pageable pageable(PageQuery pagination) {
    return PageRequest.of(pagination.page(), pagination.size(), MANAGEMENT_SORT);
  }

  private RuntimeException translateConflict(DataIntegrityViolationException exception) {

    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {

      if (cause instanceof ConstraintViolationException violation
          && "23505".equals(violation.getSQLException().getSQLState())
          && "uq_hotels_slug".equals(violation.getConstraintName())) {

        return new HotelException(HotelErrorCode.HOTEL_SLUG_CONFLICT);
      }
    }

    return exception;
  }
}

package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.RoomPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRoomRepositoryAdapter implements RoomRepository {

  private final RoomJpaRepository roomJpaRepository;

  @Override
  public Room add(Room room) {
    var entity = RoomPersistenceMapper.toNewEntity(room);

    try {
      var saved = roomJpaRepository.saveAndFlush(entity);
      return RoomPersistenceMapper.toDomain(saved);
    } catch (DataIntegrityViolationException exception) {
      throw translateConflict(exception);
    }
  }

  @Override
  public PageResult<Room> findByHotelId(UUID hotelId, PageQuery pagination) {
    return PageResultMapper.from(
        roomJpaRepository.findManagementPage(hotelId, pageable(pagination)),
        RoomPersistenceMapper::toDomain);
  }

  @Override
  public PageResult<Room> findByHotelIdAndRoomTypeId(
      UUID hotelId, UUID roomTypeId, PageQuery pagination) {
    return PageResultMapper.from(
        roomJpaRepository.findManagementPageByRoomType(hotelId, roomTypeId, pageable(pagination)),
        RoomPersistenceMapper::toDomain);
  }

  // Không gắn Sort: thứ tự đã cố định trong JPQL của RoomJpaRepository.
  private static Pageable pageable(PageQuery pagination) {
    return PageRequest.of(pagination.page(), pagination.size());
  }

  private RuntimeException translateConflict(DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation
          && "23505".equals(violation.getSQLException().getSQLState())
          && "uq_rooms_hotel_room_number".equals(violation.getConstraintName())) {
        return new HotelException(HotelErrorCode.ROOM_NUMBER_CONFLICT);
      }
    }

    return exception;
  }
}

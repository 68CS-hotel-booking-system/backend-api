package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Room;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.mapper.RoomPersistenceMapper;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

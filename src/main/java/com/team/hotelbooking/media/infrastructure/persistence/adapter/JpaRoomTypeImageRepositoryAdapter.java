package com.team.hotelbooking.media.infrastructure.persistence.adapter;

import com.team.hotelbooking.media.domain.model.RoomTypeImage;
import com.team.hotelbooking.media.domain.repository.RoomTypeImageRepository;
import com.team.hotelbooking.media.infrastructure.persistence.entity.MediaJpaEntity;
import com.team.hotelbooking.media.infrastructure.persistence.entity.RoomTypeImageJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRoomTypeImageRepositoryAdapter implements RoomTypeImageRepository {
  private final EntityManager entityManager;

  @Override
  public Optional<RoomTypeImage> findLink(UUID hotelId, UUID roomTypeId, UUID mediaId) {
    return entityManager
        .createQuery(
            """
            select i from RoomTypeImageJpaEntity i
            where i.hotelId = :hotelId and i.roomTypeId = :roomTypeId and i.mediaId = :mediaId
            """,
            RoomTypeImageJpaEntity.class)
        .setParameter("hotelId", hotelId)
        .setParameter("roomTypeId", roomTypeId)
        .setParameter("mediaId", mediaId)
        .getResultList()
        .stream()
        .findFirst()
        .map(JpaRoomTypeImageRepositoryAdapter::toDomain);
  }

  @Override
  public List<RoomTypeImage> findReadyImages(UUID hotelId, UUID roomTypeId) {
    return entityManager
        .createQuery(
            """
            select i from RoomTypeImageJpaEntity i, MediaJpaEntity m
            where i.hotelId = :hotelId and i.roomTypeId = :roomTypeId
              and m.id = i.mediaId and m.hotelId = :hotelId
              and m.status = :ready and m.purpose = 'HOTEL_IMAGE'
            order by i.createdAt, i.id
            """,
            RoomTypeImageJpaEntity.class)
        .setParameter("hotelId", hotelId)
        .setParameter("roomTypeId", roomTypeId)
        .setParameter("ready", MediaJpaEntity.DbStatus.READY)
        .getResultList()
        .stream()
        .map(JpaRoomTypeImageRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public List<UUID> findLinkedRoomTypes(UUID hotelId, UUID mediaId) {
    return entityManager
        .createQuery(
            """
            select i.roomTypeId from RoomTypeImageJpaEntity i
            where i.hotelId = :hotelId and i.mediaId = :mediaId
            """,
            UUID.class)
        .setParameter("hotelId", hotelId)
        .setParameter("mediaId", mediaId)
        .getResultList();
  }

  @Override
  public void add(RoomTypeImage image) {
    RoomTypeImageJpaEntity entity = new RoomTypeImageJpaEntity();
    entity.setId(image.id());
    entity.setHotelId(image.hotelId());
    entity.setRoomTypeId(image.roomTypeId());
    entity.setMediaId(image.mediaId());
    entity.setCreatedAt(image.createdAt());
    entityManager.persist(entity);
  }

  private static RoomTypeImage toDomain(RoomTypeImageJpaEntity entity) {
    return new RoomTypeImage(
        entity.getId(),
        entity.getHotelId(),
        entity.getRoomTypeId(),
        entity.getMediaId(),
        entity.getCreatedAt());
  }
}

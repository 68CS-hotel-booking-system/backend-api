package com.team.hotelbooking.media.infrastructure.persistence.adapter;

import com.team.hotelbooking.media.domain.model.HotelImage;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageId;
import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageJpaEntity;
import com.team.hotelbooking.media.infrastructure.persistence.entity.MediaJpaEntity;
import com.team.hotelbooking.media.infrastructure.persistence.mapper.MediaPersistenceMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaHotelImageRepositoryAdapter implements HotelImageRepository {

  private final EntityManager entityManager;

  @Override
  public List<HotelImage> findByHotelId(UUID hotelId) {
    return entityManager
        .createQuery(
            """
          select i from HotelImageJpaEntity i
          where i.id.hotelId = :hotelId
          order by i.displayOrder asc, i.id.mediaId asc
          """,
            HotelImageJpaEntity.class)
        .setParameter("hotelId", hotelId)
        .getResultList()
        .stream()
        .map(
            image ->
                new HotelImage(
                    image.getId().getMediaId(),
                    image.getAlbumId(),
                    image.getDisplayOrder(),
                    image.isCover()))
        .toList();
  }

  @Override
  public List<MediaObject> lockMedia(List<UUID> mediaIds) {
    if (mediaIds.isEmpty()) {
      return List.of();
    }

    return entityManager
        .createQuery(
            """
          select m from MediaJpaEntity m
          where m.id in :ids
          order by m.id asc
          """,
            MediaJpaEntity.class)
        .setParameter("ids", mediaIds)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .getResultList()
        .stream()
        .map(MediaPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public boolean isLinked(UUID hotelId, UUID mediaId) {
    Long count =
        entityManager
            .createQuery(
                """
            select count(i) from HotelImageJpaEntity i
            where i.id.hotelId = :hotelId
              and i.id.mediaId = :mediaId
            """,
                Long.class)
            .setParameter("hotelId", hotelId)
            .setParameter("mediaId", mediaId)
            .getSingleResult();

    return count > 0;
  }

  @Override
  public void replace(UUID hotelId, List<HotelImage> images, Instant createdAt) {

    entityManager.flush();

    entityManager
        .createQuery(
            """
          delete from HotelImageJpaEntity i
          where i.id.hotelId = :hotelId
          """)
        .setParameter("hotelId", hotelId)
        .executeUpdate();

    for (HotelImage image : images) {
      HotelImageJpaEntity entity = new HotelImageJpaEntity();

      entity.setId(new HotelImageId(hotelId, image.mediaId()));
      entity.setAlbumId(image.albumId());
      entity.setDisplayOrder(image.sortOrder());
      entity.setCover(image.cover());
      entity.setCreatedAt(createdAt);

      entityManager.persist(entity);
    }

    entityManager.flush();
  }
}

package com.team.hotelbooking.media.infrastructure.persistence.adapter;

import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.repository.MediaRepository;
import com.team.hotelbooking.media.infrastructure.persistence.entity.MediaJpaEntity;
import com.team.hotelbooking.media.infrastructure.persistence.mapper.MediaPersistenceMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaMediaRepositoryAdapter implements MediaRepository {

  private final EntityManager entityManager;

  @Override
  public void add(MediaObject media) {
    entityManager.persist(MediaPersistenceMapper.toNewEntity(media));
    entityManager.flush();
  }

  @Override
  public Optional<MediaObject> findById(UUID mediaId) {
    return Optional.ofNullable(entityManager.find(MediaJpaEntity.class, mediaId))
        .map(MediaPersistenceMapper::toDomain);
  }

  @Override
  public void markReady(UUID mediaId) {
    int updated =
        entityManager
            .createQuery(
                """
            update MediaJpaEntity m
            set m.status = :ready
            where m.id = :id and m.status = :pending
            """)
            .setParameter("ready", MediaJpaEntity.DbStatus.READY)
            .setParameter("pending", MediaJpaEntity.DbStatus.PENDING)
            .setParameter("id", mediaId)
            .executeUpdate();

    if (updated != 1) {
      throw new MediaException(MediaErrorCode.CONFLICT);
    }
  }

  @Override
  public void markDeleted(UUID mediaId, Instant deletedAt) {
    int updated =
        entityManager
            .createQuery(
                """
            update MediaJpaEntity m
            set m.status = :deleted, m.deletedAt = :deletedAt
            where m.id = :id and m.status = :pending
            """)
            .setParameter("deleted", MediaJpaEntity.DbStatus.DELETED)
            .setParameter("pending", MediaJpaEntity.DbStatus.PENDING)
            .setParameter("deletedAt", deletedAt)
            .setParameter("id", mediaId)
            .executeUpdate();

    if (updated != 1) {
      throw new MediaException(MediaErrorCode.CONFLICT);
    }
  }
}

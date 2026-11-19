package com.team.hotelbooking.media.infrastructure.persistence.adapter;

import com.team.hotelbooking.media.domain.repository.UserAvatarRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaUserAvatarRepositoryAdapter implements UserAvatarRepository {
  private final EntityManager entityManager;

  @Override
  public Optional<UUID> findMediaId(UUID userId) {
    List<?> rows =
        entityManager
            .createNativeQuery(
                """
            select a.media_object_id from user_avatars a
            join media_objects m on m.id = a.media_object_id
            where a.user_id = :userId and m.status = 'READY'
            """,
                UUID.class)
            .setParameter("userId", userId)
            .getResultList();
    return rows.stream().map(UUID.class::cast).findFirst();
  }

  @Override
  public void setAvatar(UUID userId, UUID mediaId) {
    entityManager
        .createNativeQuery(
            """
            insert into user_avatars (user_id, media_object_id) values (:userId, :mediaId)
            on conflict (user_id) do update set media_object_id = excluded.media_object_id
            """)
        .setParameter("userId", userId)
        .setParameter("mediaId", mediaId)
        .executeUpdate();
  }
}

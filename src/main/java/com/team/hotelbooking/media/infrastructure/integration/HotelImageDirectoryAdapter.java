package com.team.hotelbooking.media.infrastructure.integration;

import com.team.hotelbooking.media.HotelImageDirectory;
import com.team.hotelbooking.media.infrastructure.persistence.entity.MediaJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

// Chỉ đọc DB nên không phụ thuộc storage.r2.enabled; tham gia transaction của bên gọi.
@Component
public class HotelImageDirectoryAdapter implements HotelImageDirectory {

  private final EntityManager entityManager;

  public HotelImageDirectoryAdapter(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Map<UUID, UUID> findCoverImages(Collection<UUID> hotelIds) {
    if (hotelIds == null || hotelIds.isEmpty()) {
      return Map.of();
    }
    List<Object[]> rows =
        entityManager
            .createQuery(
                """
                SELECT i.id.hotelId, i.id.mediaId
                FROM HotelImageJpaEntity i, MediaJpaEntity m
                WHERE m.id = i.id.mediaId
                  AND i.id.hotelId IN :hotelIds
                  AND i.cover = true
                  AND m.status = :ready
                """,
                Object[].class)
            .setParameter("hotelIds", hotelIds)
            .setParameter("ready", MediaJpaEntity.DbStatus.READY)
            .getResultList();
    Map<UUID, UUID> covers = new HashMap<>();
    for (Object[] row : rows) {
      covers.put((UUID) row[0], (UUID) row[1]);
    }
    return covers;
  }
}

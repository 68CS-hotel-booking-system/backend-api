package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.CityJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn theo lô phục vụ tìm kiếm khách sạn (module inventory gọi qua HotelDirectory). Mỗi hàm là
 * một truy vấn cho cả danh sách để tìm kiếm không phát sinh N+1.
 */
@Repository
public class HotelSearchJpaQueries {

  private final EntityManager entityManager;

  public HotelSearchJpaQueries(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  // Lọc tĩnh: chỉ ACTIVE (HT-05); có đủ MỌI tiện ích khách sạn được yêu cầu.
  public List<Object[]> findActiveHotelsWithCity(
      UUID cityId, String citySlug, Integer minStarRating, Set<UUID> amenityIds) {
    StringBuilder jpql =
        new StringBuilder(
            """
            SELECT h, c FROM HotelJpaEntity h, CityJpaEntity c
            WHERE c.id = h.cityId AND h.status = :active
            """);
    if (cityId != null) {
      jpql.append(" AND h.cityId = :cityId");
    }
    if (citySlug != null) {
      jpql.append(" AND c.slug = :citySlug");
    }
    if (minStarRating != null) {
      jpql.append(" AND h.starRating >= :minStarRating");
    }
    if (!amenityIds.isEmpty()) {
      jpql.append(
          """
           AND h.id IN (
             SELECT ha.id.hotelId FROM HotelAmenityJpaEntity ha
             WHERE ha.id.amenityId IN :amenityIds
             GROUP BY ha.id.hotelId
             HAVING COUNT(DISTINCT ha.id.amenityId) = :amenityCount)
          """);
    }

    TypedQuery<Object[]> query =
        entityManager
            .createQuery(jpql.toString(), Object[].class)
            .setParameter("active", HotelJpaEntity.DbStatus.ACTIVE);
    if (cityId != null) {
      query.setParameter("cityId", cityId);
    }
    if (citySlug != null) {
      query.setParameter("citySlug", citySlug);
    }
    if (minStarRating != null) {
      query.setParameter("minStarRating", minStarRating.shortValue());
    }
    if (!amenityIds.isEmpty()) {
      query.setParameter("amenityIds", amenityIds);
      query.setParameter("amenityCount", (long) amenityIds.size());
    }
    return query.getResultList();
  }

  public static HotelJpaEntity hotelOf(Object[] row) {
    return (HotelJpaEntity) row[0];
  }

  public static CityJpaEntity cityOf(Object[] row) {
    return (CityJpaEntity) row[1];
  }

  // Mỗi dòng: hotelId, amenityId, code, name; sắp theo tên để tóm tắt ổn định.
  public List<Object[]> findHotelAmenities(Collection<UUID> hotelIds) {
    if (hotelIds.isEmpty()) {
      return List.of();
    }
    return entityManager
        .createQuery(
            """
            SELECT ha.id.hotelId, a.id, a.code, a.name
            FROM HotelAmenityJpaEntity ha, AmenityJpaEntity a
            WHERE a.id = ha.id.amenityId AND ha.id.hotelId IN :hotelIds
            ORDER BY a.name, a.id
            """,
            Object[].class)
        .setParameter("hotelIds", hotelIds)
        .getResultList();
  }

  public List<RoomTypeJpaEntity> findActiveRoomTypesByHotelIds(Collection<UUID> hotelIds) {
    if (hotelIds.isEmpty()) {
      return List.of();
    }
    return entityManager
        .createQuery(
            """
            SELECT rt FROM RoomTypeJpaEntity rt
            WHERE rt.hotelId IN :hotelIds AND rt.active = true
            ORDER BY rt.name, rt.id
            """,
            RoomTypeJpaEntity.class)
        .setParameter("hotelIds", hotelIds)
        .getResultList();
  }

  // Cùng điều kiện báo giá: loại phòng đang bán và khách sạn của nó đang ACTIVE.
  public List<RoomTypeJpaEntity> findActiveRoomTypesOfActiveHotels(Collection<UUID> roomTypeIds) {
    if (roomTypeIds.isEmpty()) {
      return List.of();
    }
    return entityManager
        .createQuery(
            """
            SELECT rt FROM RoomTypeJpaEntity rt, HotelJpaEntity h
            WHERE h.id = rt.hotelId AND rt.id IN :roomTypeIds
              AND rt.active = true AND h.status = :active
            """,
            RoomTypeJpaEntity.class)
        .setParameter("roomTypeIds", roomTypeIds)
        .setParameter("active", HotelJpaEntity.DbStatus.ACTIVE)
        .getResultList();
  }

  // Đang phục vụ tại thời điểm at: đã tới operationalFrom và chưa tới operationalUntil (nếu có).
  public Map<UUID, Long> countOperationalRoomsByRoomTypeIds(
      Collection<UUID> roomTypeIds, Instant at) {
    if (roomTypeIds.isEmpty()) {
      return Map.of();
    }
    List<Object[]> rows =
        entityManager
            .createQuery(
                """
                SELECT r.roomTypeId, COUNT(r) FROM RoomJpaEntity r
                WHERE r.roomTypeId IN :roomTypeIds
                  AND r.operationalFrom <= :at
                  AND (r.operationalUntil IS NULL OR r.operationalUntil > :at)
                GROUP BY r.roomTypeId
                """,
                Object[].class)
            .setParameter("roomTypeIds", roomTypeIds)
            .setParameter("at", at)
            .getResultList();
    Map<UUID, Long> counts = new HashMap<>();
    for (Object[] row : rows) {
      counts.put((UUID) row[0], (Long) row[1]);
    }
    return counts;
  }
}

package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class HotelSpecs {

  private HotelSpecs() {}

  public static Specification<HotelJpaEntity> publicSearch(UUID cityId, String keyword) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(cb.equal(root.get("status"), HotelJpaEntity.DbStatus.ACTIVE));

      if (cityId != null) {
        predicates.add(cb.equal(root.get("cityId"), cityId));
      }

      if (keyword != null && !keyword.isBlank()) {
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.<String>get("name")), pattern, '\\'),
                cb.like(cb.lower(root.<String>get("address")), pattern, '\\')));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}

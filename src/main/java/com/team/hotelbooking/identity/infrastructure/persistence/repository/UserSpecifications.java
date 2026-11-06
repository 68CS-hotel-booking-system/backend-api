package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.UserSearchCriteria;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

  private UserSpecifications() {}

  public static Specification<UserJpaEntity> matching(UserSearchCriteria criteria) {

    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(builder.isFalse(root.get("deleted")));

      if (hasText(criteria.search())) {
        String keyword = criteria.search().trim().toLowerCase(Locale.ROOT);

        String pattern =
            "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";

        predicates.add(
            builder.or(
                builder.like(builder.lower(root.<String>get("fullName")), pattern, '!'),
                builder.like(builder.lower(root.<String>get("email")), pattern, '!'),
                builder.like(root.<String>get("phone"), pattern, '!')));
      }

      if (hasText(criteria.status())) {
        predicates.add(builder.equal(root.get("status"), toDbStatus(criteria.status())));
      }

      if (criteria.createdFrom() != null) {
        predicates.add(
            builder.greaterThanOrEqualTo(root.<Instant>get("createdAt"), criteria.createdFrom()));
      }

      if (criteria.createdTo() != null) {
        predicates.add(builder.lessThan(root.<Instant>get("createdAt"), criteria.createdTo()));
      }

      if (hasText(criteria.role())) {
        Subquery<Integer> roleQuery = query.subquery(Integer.class);

        Root<UserRoleJpaEntity> assignment = roleQuery.from(UserRoleJpaEntity.class);

        Root<RoleJpaEntity> role = roleQuery.from(RoleJpaEntity.class);

        roleQuery
            .select(builder.literal(1))
            .where(
                builder.equal(assignment.get("userId"), root.get("id")),
                builder.equal(assignment.get("roleId"), role.get("id")),
                builder.equal(role.get("roleKey"), criteria.role().trim()),
                builder.isTrue(role.get("active")));

        predicates.add(builder.exists(roleQuery));
      }

      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static UserJpaEntity.DbStatus toDbStatus(String status) {

    try {
      return UserJpaEntity.DbStatus.valueOf(status.trim().toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw IdentityException.invalid("Trạng thái phải là active, inactive, pending hoặc banned.");
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}

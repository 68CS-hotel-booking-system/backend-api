package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.AccountContactDirectory;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityAccountContactAdapter implements AccountContactDirectory {
  private final UserJpaRepository users;

  @Override
  public PageResult<Contact> findByIds(Set<UUID> ids, String keyword, PageQuery pagination) {
    if (ids.isEmpty())
      return new PageResult<>(
          List.of(), pagination.page(), pagination.size(), 0, 0, pagination.page() == 0, true);
    String pattern =
        "%"
            + keyword
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            + "%";
    var page =
        users.findAll(
            (root, query, cb) ->
                cb.and(
                    root.get("id").in(ids),
                    cb.isFalse(root.get("deleted")),
                    cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(root.get("phone"), pattern, '\\'))),
            PageRequest.of(
                pagination.page(), pagination.size(), Sort.by("fullName").and(Sort.by("id"))));
    return new PageResult<>(
        page.getContent().stream()
            .map(u -> new Contact(u.getId(), u.getFullName(), u.getEmail(), u.getPhone()))
            .toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }
}

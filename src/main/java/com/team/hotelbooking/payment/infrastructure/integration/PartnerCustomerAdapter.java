package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.identity.AccountContactDirectory;
import com.team.hotelbooking.identity.AccountDirectory;
import com.team.hotelbooking.payment.application.port.PartnerCustomerPort;
import com.team.hotelbooking.payment.application.result.PartnerCustomerResult;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartnerCustomerAdapter implements PartnerCustomerPort {
  private final AccountDirectory accounts;
  private final AccountContactDirectory contacts;
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public boolean isActiveAdmin(UUID actorId) {
    return accounts
        .findById(actorId)
        .map(a -> a.active() && a.emailVerified() && a.roles().contains("ADMIN"))
        .orElse(false);
  }

  @Override
  public PageResult<PartnerCustomerResult> findCustomers(
      String keyword, PageQuery pagination, Instant now) {
    var ids =
        new HashSet<>(
            jdbc.query(
                "SELECT DISTINCT owner_user_id FROM hotel_contracts WHERE has_been_activated",
                Map.of(),
                (row, index) -> row.getObject("owner_user_id", UUID.class)));
    var page = contacts.findByIds(ids, keyword, pagination);
    if (page.content().isEmpty())
      return new PageResult<>(
          java.util.List.of(),
          page.page(),
          page.size(),
          page.totalElements(),
          page.totalPages(),
          page.first(),
          page.last());
    record Summary(long hotelCount, long contractCount, long activeCount, Instant lastCreatedAt) {}
    var summaries = new HashMap<UUID, Summary>();
    jdbc.query(
        """
        SELECT owner_user_id, count(DISTINCT hotel_id) AS hotel_count, count(*) AS contract_count,
          count(*) FILTER (WHERE status = 'ACTIVE' AND starts_at <= :now AND ends_at > :now) AS active_count,
          max(created_at) AS last_created_at FROM hotel_contracts
        WHERE has_been_activated AND owner_user_id IN (:ids) GROUP BY owner_user_id
        """,
        Map.of(
            "ids",
            page.content().stream().map(AccountContactDirectory.Contact::id).toList(),
            "now",
            java.sql.Timestamp.from(now)),
        row -> {
          summaries.put(
              row.getObject("owner_user_id", UUID.class),
              new Summary(
                  row.getLong("hotel_count"),
                  row.getLong("contract_count"),
                  row.getLong("active_count"),
                  row.getTimestamp("last_created_at").toInstant()));
        });
    return page.map(
        c -> {
          var summary = summaries.get(c.id());
          return new PartnerCustomerResult(
              c.id(),
              c.fullName(),
              c.email(),
              c.phone(),
              summary.hotelCount(),
              summary.contractCount(),
              summary.activeCount(),
              summary.lastCreatedAt());
        });
  }
}

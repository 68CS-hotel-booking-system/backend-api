package com.team.hotelbooking.booking.complaint.infrastructure.persistence.adapter;

import com.team.hotelbooking.booking.complaint.domain.model.*;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintCaseRepository;
import com.team.hotelbooking.shared.pagination.*;
import java.sql.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcComplaintCaseRepositoryAdapter implements ComplaintCaseRepository {
  private final NamedParameterJdbcTemplate jdbc;
  private static final String SELECT =
      "SELECT c.*, b.booking_code, b.booker_name_snapshot, b.booker_email_snapshot, b.booker_phone_snapshot ";
  private static final String JOIN = " FROM complaints c JOIN bookings b ON b.id = c.booking_id ";

  public PageResult<ComplaintCase> findCases(
      Set<UUID> hotelIds, Complaint.Status status, String keyword, PageQuery page) {
    if (hotelIds.isEmpty())
      return new PageResult<>(List.of(), page.page(), page.size(), 0, 0, page.page() == 0, true);
    var params =
        new MapSqlParameterSource("ids", hotelIds)
            .addValue("status", status == null ? null : status.name())
            .addValue(
                "pattern",
                "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%")
            .addValue("limit", page.size())
            .addValue("offset", (long) page.page() * page.size());
    String where =
        " WHERE c.hotel_id IN (:ids) "
            + (status == null ? "" : " AND c.status::text = :status ")
            + " AND (b.booking_code ILIKE :pattern ESCAPE '!' OR b.booker_name_snapshot ILIKE :pattern ESCAPE '!' OR c.content ILIKE :pattern ESCAPE '!') ";
    long total =
        Objects.requireNonNull(
            jdbc.queryForObject("SELECT count(*)" + JOIN + where, params, Long.class));
    var rows =
        jdbc.query(
            SELECT
                + JOIN
                + where
                + " ORDER BY c.created_at DESC, c.id DESC LIMIT :limit OFFSET :offset",
            params,
            (row, index) -> map(row));
    int pages = (int) ((total + page.size() - 1) / page.size());
    return new PageResult<>(
        rows,
        page.page(),
        page.size(),
        total,
        pages,
        page.page() == 0,
        (long) page.page() + 1 >= pages);
  }

  public Optional<ComplaintCase> findCase(UUID id) {
    return jdbc
        .query(SELECT + JOIN + " WHERE c.id = :id", Map.of("id", id), (row, index) -> map(row))
        .stream()
        .findFirst();
  }

  private static ComplaintCase map(ResultSet row) throws SQLException {
    var complaint =
        new Complaint(
            row.getObject("id", UUID.class),
            row.getObject("booking_id", UUID.class),
            row.getObject("customer_id", UUID.class),
            row.getObject("hotel_id", UUID.class),
            Complaint.Category.valueOf(row.getString("category")),
            row.getString("content"),
            Complaint.Status.valueOf(row.getString("status")),
            row.getTimestamp("created_at").toInstant(),
            row.getTimestamp("updated_at").toInstant(),
            row.getLong("version"));
    return new ComplaintCase(
        complaint,
        row.getString("booking_code"),
        row.getString("booker_name_snapshot"),
        row.getString("booker_email_snapshot"),
        row.getString("booker_phone_snapshot"));
  }

  public List<ComplaintAction> history(UUID id) {
    return jdbc.query(
        "SELECT * FROM complaint_actions WHERE complaint_id = :id ORDER BY version",
        Map.of("id", id),
        (row, index) ->
            new ComplaintAction(
                row.getObject("id", UUID.class),
                row.getObject("complaint_id", UUID.class),
                row.getObject("actor_user_id", UUID.class),
                row.getString("actor_role"),
                Complaint.Status.valueOf(row.getString("from_status")),
                Complaint.Status.valueOf(row.getString("to_status")),
                row.getString("response"),
                row.getTimestamp("created_at").toInstant(),
                row.getLong("version")));
  }

  public boolean update(Complaint complaint, long expectedVersion, ComplaintAction action) {
    var params =
        new MapSqlParameterSource("id", complaint.id())
            .addValue("status", complaint.status().name())
            .addValue("now", Timestamp.from(complaint.updatedAt()))
            .addValue("expected", expectedVersion)
            .addValue("version", complaint.version());
    int updated =
        jdbc.update(
            "UPDATE complaints SET status = CAST(:status AS complaint_status__t), updated_at = :now, version = :version WHERE id = :id AND version = :expected",
            params);
    if (updated != 1) return false;
    jdbc.update(
        """
        INSERT INTO complaint_actions (id,complaint_id,actor_user_id,actor_role,from_status,to_status,response,created_at,version)
        VALUES (:id,:complaint,:actor,:role,CAST(:from AS complaint_status__t),CAST(:to AS complaint_status__t),:response,:now,:version)
        """,
        new MapSqlParameterSource("id", action.id())
            .addValue("complaint", action.complaintId())
            .addValue("actor", action.actorUserId())
            .addValue("role", action.actorRole())
            .addValue("from", action.fromStatus().name())
            .addValue("to", action.toStatus().name())
            .addValue("response", action.response())
            .addValue("now", Timestamp.from(action.createdAt()))
            .addValue("version", action.version()));
    return true;
  }
}

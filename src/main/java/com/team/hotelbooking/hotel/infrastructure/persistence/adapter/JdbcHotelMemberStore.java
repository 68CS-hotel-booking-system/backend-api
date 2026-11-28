package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcHotelMemberStore implements HotelMemberStore {

  private static final String SELECT_MEMBER =
      """
      SELECT hm.id, hm.hotel_id, hm.user_id, u.full_name, u.email,
             hm.role::text AS role, hm.is_active, hm.assigned_at, hm.updated_at, hm.version
      FROM hotel_members hm
      JOIN users u ON u.id = hm.user_id
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public PageResult<MemberRecord> pageByHotel(UUID hotelId, PageQuery pagination) {
    Long totalObj =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM hotel_members WHERE hotel_id = :hotelId",
            Map.of("hotelId", hotelId),
            Long.class);
    long total = totalObj == null ? 0L : totalObj;
    if (total == 0L) {
      return emptyPage(pagination);
    }
    var params =
        new MapSqlParameterSource()
            .addValue("hotelId", hotelId)
            .addValue("limit", pagination.size())
            .addValue("offset", (long) pagination.page() * pagination.size());
    List<MemberRecord> rows =
        jdbc.query(
            SELECT_MEMBER
                + """
                WHERE hm.hotel_id = :hotelId
                ORDER BY hm.is_active DESC, hm.assigned_at DESC, hm.id
                LIMIT :limit OFFSET :offset
                """,
            params,
            JdbcHotelMemberStore::mapMember);
    return toPage(rows, pagination, total);
  }

  @Override
  public Optional<MemberRecord> findById(UUID hotelId, UUID memberId) {
    List<MemberRecord> rows =
        jdbc.query(
            SELECT_MEMBER + "WHERE hm.hotel_id = :hotelId AND hm.id = :memberId",
            Map.of("hotelId", hotelId, "memberId", memberId),
            JdbcHotelMemberStore::mapMember);
    return rows.stream().findFirst();
  }

  @Override
  public Optional<MemberRecord> findByHotelUserRole(
      UUID hotelId, UUID userId, HotelMemberRole role) {
    List<MemberRecord> rows =
        jdbc.query(
            SELECT_MEMBER
                + """
                WHERE hm.hotel_id = :hotelId
                  AND hm.user_id = :userId
                  AND hm.role = CAST(:role AS hotel_member_role__t)
                """,
            Map.of("hotelId", hotelId, "userId", userId, "role", role.name()),
            JdbcHotelMemberStore::mapMember);
    return rows.stream().findFirst();
  }

  @Override
  public boolean hasOtherActiveMembership(UUID hotelId, UUID userId, UUID excludeMemberId) {
    var params = new MapSqlParameterSource().addValue("hotelId", hotelId).addValue("userId", userId);
    String sql =
        "SELECT EXISTS (SELECT 1 FROM hotel_members WHERE hotel_id = :hotelId AND user_id = :userId AND is_active = true";
    if (excludeMemberId != null) {
      sql += " AND id <> :excludeId";
      params.addValue("excludeId", excludeMemberId);
    }
    sql += ")";
    Boolean exists = jdbc.queryForObject(sql, params, Boolean.class);
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public MemberRecord insert(
      UUID hotelId, UUID userId, HotelMemberRole role, UUID actorId, Instant now) {
    UUID id = UUID.randomUUID();
    Timestamp ts = Timestamp.from(now);
    var params =
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("hotelId", hotelId)
            .addValue("userId", userId)
            .addValue("role", role.name())
            .addValue("now", ts)
            .addValue("actorId", actorId);
    jdbc.update(
        """
        INSERT INTO hotel_members (id, hotel_id, user_id, role, is_active, assigned_at, updated_at, updated_by, version)
        VALUES (:id, :hotelId, :userId, CAST(:role AS hotel_member_role__t), true, :now, :now, :actorId, 0)
        """,
        params);
    return findById(hotelId, id).orElseThrow();
  }

  @Override
  public int updateStatus(
      UUID memberId, boolean active, long expectedVersion, UUID actorId, Instant now) {
    Timestamp ts = Timestamp.from(now);
    var params =
        new MapSqlParameterSource()
            .addValue("memberId", memberId)
            .addValue("active", active)
            .addValue("expectedVersion", expectedVersion)
            .addValue("actorId", actorId)
            .addValue("now", ts);
    String sql =
        active
            ? """
              UPDATE hotel_members
              SET is_active = true, assigned_at = :now, updated_at = :now,
                  updated_by = :actorId, version = version + 1
              WHERE id = :memberId AND version = :expectedVersion
              """
            : """
              UPDATE hotel_members
              SET is_active = false, updated_at = :now,
                  updated_by = :actorId, version = version + 1
              WHERE id = :memberId AND version = :expectedVersion
              """;
    return jdbc.update(sql, params);
  }

  @Override
  public PageResult<StaffHotelRecord> pageStaffHotels(UUID actorId, PageQuery pagination) {
    String filter =
        """
        FROM hotels h
        WHERE h.owner_user_id = :actorId
           OR EXISTS (
             SELECT 1 FROM hotel_members hm
             WHERE hm.hotel_id = h.id
               AND hm.user_id = :actorId
               AND hm.role = 'HOTEL_MANAGER'
               AND hm.is_active = true
           )
        """;
    Long totalObj =
        jdbc.queryForObject("SELECT COUNT(*) " + filter, Map.of("actorId", actorId), Long.class);
    long total = totalObj == null ? 0L : totalObj;
    if (total == 0L) {
      return emptyPage(pagination);
    }
    var params =
        new MapSqlParameterSource()
            .addValue("actorId", actorId)
            .addValue("limit", pagination.size())
            .addValue("offset", (long) pagination.page() * pagination.size());
    List<StaffHotelRecord> rows =
        jdbc.query(
            "SELECT h.id, h.name, (h.owner_user_id = :actorId) AS can_assign_manager "
                + filter
                + "ORDER BY h.name, h.id LIMIT :limit OFFSET :offset",
            params,
            (rs, idx) ->
                new StaffHotelRecord(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getBoolean("can_assign_manager")));
    return toPage(rows, pagination, total);
  }

  @Override
  public List<WorkplaceRecord> findWorkplaces(UUID userId) {
    return jdbc.query(
        """
        SELECT hm.hotel_id, h.name AS hotel_name, hm.role::text AS role
        FROM hotel_members hm
        JOIN hotels h ON h.id = hm.hotel_id
        WHERE hm.user_id = :userId AND hm.is_active = true
        ORDER BY h.name, hm.hotel_id, hm.role
        """,
        Map.of("userId", userId),
        (rs, idx) ->
            new WorkplaceRecord(
                rs.getObject("hotel_id", UUID.class),
                rs.getString("hotel_name"),
                HotelMemberRole.valueOf(rs.getString("role"))));
  }

  @Override
  public boolean hasAnyActiveMembership(UUID userId) {
    Boolean exists =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM hotel_members WHERE user_id = :userId AND is_active = true)",
            Map.of("userId", userId),
            Boolean.class);
    return Boolean.TRUE.equals(exists);
  }

  private static MemberRecord mapMember(ResultSet rs, int index) throws SQLException {
    return new MemberRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("hotel_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getString("full_name"),
        rs.getString("email"),
        HotelMemberRole.valueOf(rs.getString("role")),
        rs.getBoolean("is_active"),
        rs.getTimestamp("assigned_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(),
        rs.getLong("version"));
  }

  private static <T> PageResult<T> emptyPage(PageQuery pagination) {
    return new PageResult<>(
        List.of(), pagination.page(), pagination.size(), 0L, 0, pagination.page() == 0, true);
  }

  private static <T> PageResult<T> toPage(List<T> rows, PageQuery pagination, long total) {
    int totalPages = (int) ((total + pagination.size() - 1) / pagination.size());
    return new PageResult<>(
        rows,
        pagination.page(),
        pagination.size(),
        total,
        totalPages,
        pagination.page() == 0,
        (long) pagination.page() + 1 >= totalPages);
  }
}

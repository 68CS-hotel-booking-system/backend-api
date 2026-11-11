package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tìm kiếm trên PostgreSQL thật, gọi qua HTTP không đăng nhập. Fixture dùng thành phố riêng để
 * không lẫn dữ liệu của lớp test khác trên cùng DB.
 *
 * <ul>
 *   <li>Alpha ACTIVE 4★ (đỗ xe + hồ bơi): đủ tồn và giá; đêm 06-02 có giá riêng; có ảnh bìa READY.
 *   <li>Bravo ACTIVE 5★: 1 phòng bị chặn đêm 06-02 → hết phòng đêm đó.
 *   <li>Charlie ACTIVE 3★: base_price 0, chỉ có giá riêng đêm 06-01 → thiếu giá đêm 06-02.
 *   <li>Delta ACTIVE 3★: loại phòng 1 khách; ảnh bìa còn PENDING → không có ảnh bìa.
 *   <li>Echo DRAFT, Foxtrot SUSPENDED: đủ dữ liệu nhưng không được hiện.
 *   <li>Golf ACTIVE: có loại phòng nhưng không có phòng vật lý đang phục vụ.
 * </ul>
 */
// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GET /api/v1/hotels/search trên PostgreSQL (HT-05, IV-13, PR-02, MD-05)")
class HotelSearchIntegrationTests {

  private static final String CITY_SLUG = "search-it-city";
  private static final UUID CITY_ID = uuid("0c", 1);
  private static final UUID OWNER_ID = uuid("0a", 1);
  private static final UUID ALPHA = uuid("0b", 1);
  private static final UUID BRAVO = uuid("0b", 2);
  private static final UUID CHARLIE = uuid("0b", 3);
  private static final UUID DELTA = uuid("0b", 4);
  private static final UUID ECHO = uuid("0b", 5);
  private static final UUID FOXTROT = uuid("0b", 6);
  private static final UUID GOLF = uuid("0b", 7);
  private static final UUID ALPHA_COVER = uuid("0d", 1);
  private static final UUID DELTA_COVER = uuid("0d", 2);
  private static final UUID FREE_PARKING = UUID.fromString("59fe1090-6e67-4d93-b140-232c5e885c27");
  private static final UUID SWIMMING_POOL = UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3");
  private static final LocalDate NIGHT_1 = LocalDate.of(2027, 6, 1);
  private static final LocalDate NIGHT_2 = LocalDate.of(2027, 6, 2);

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManagerFactory entityManagerFactory;

  // Số câu SQL Hibernate chuẩn bị trong một lần tìm kiếm (thống kê bật tạm cho riêng phép đo).
  private long statementsFor(MockHttpServletRequestBuilder request, int expectedHotels)
      throws Exception {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    try {
      search(request)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(expectedHotels))
          .andExpect(jsonPath("$.data.content", hasSize(expectedHotels)));
      return statistics.getPrepareStatementCount();
    } finally {
      statistics.setStatisticsEnabled(false);
    }
  }

  private static UUID uuid(String group, int n) {
    return UUID.fromString(String.format("5ea4c000-00%s-4000-8000-%012d", group, n));
  }

  @BeforeEach
  void setUp() {
    jdbc.update(
        """
        INSERT INTO cities (id, country_code, name, slug)
        VALUES (?, 'VN', 'Thành phố tìm kiếm test', ?) ON CONFLICT (id) DO NOTHING
        """,
        CITY_ID,
        CITY_SLUG);
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, 'search-it-owner@test.local', 'not-a-real-hash', 'Owner tìm kiếm',
                '+84956000001', 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        OWNER_ID);

    hotel(ALPHA, "Alpha", 4, "ACTIVE");
    hotel(BRAVO, "Bravo", 5, "ACTIVE");
    hotel(CHARLIE, "Charlie", 3, "ACTIVE");
    hotel(DELTA, "Delta", 3, "ACTIVE");
    hotel(ECHO, "Echo", 5, "DRAFT");
    hotel(FOXTROT, "Foxtrot", 5, "SUSPENDED");
    hotel(GOLF, "Golf", 4, "ACTIVE");

    UUID alphaRoomType = roomType(ALPHA, 2, 1_000_000L, 2);
    UUID bravoRoomType = roomType(BRAVO, 2, 2_000_000L, 1);
    UUID charlieRoomType = roomType(CHARLIE, 4, 0L, 1);
    roomType(DELTA, 1, 500_000L, 1);
    roomType(ECHO, 2, 300_000L, 1);
    roomType(FOXTROT, 2, 300_000L, 1);
    roomType(GOLF, 2, 300_000L, 0);

    rate(alphaRoomType, NIGHT_2, 1_200_000L);
    rate(charlieRoomType, NIGHT_1, 800_000L);
    jdbc.update("DELETE FROM room_blocks WHERE room_type_id = ?", bravoRoomType);
    jdbc.update(
        """
        INSERT INTO room_blocks
          (hotel_id, room_type_id, room_id, start_date, end_date, status, created_by_user_id)
        SELECT r.hotel_id, r.room_type_id, r.id, ?, ?, CAST('ACTIVE' AS room_block_status__t), ? FROM rooms r
        WHERE r.room_type_id = ?
        """,
        NIGHT_2,
        NIGHT_2,
        OWNER_ID,
        bravoRoomType);

    jdbc.update(
        """
        INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES (?, ?), (?, ?), (?, ?)
        ON CONFLICT DO NOTHING
        """,
        ALPHA,
        FREE_PARKING,
        ALPHA,
        SWIMMING_POOL,
        BRAVO,
        FREE_PARKING);

    coverImage(ALPHA, ALPHA_COVER, "READY");
    coverImage(DELTA, DELTA_COVER, "PENDING");
  }

  private void hotel(UUID id, String name, int stars, String status) {
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, contact_phone,
                            star_rating, status)
        VALUES (?, ?, ?, ?, ?, 'Địa chỉ test', '+84905000000', ?, CAST(? AS hotel_status__t))
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        OWNER_ID,
        CITY_ID,
        name,
        "search-it-" + name.toLowerCase(),
        stars,
        status);
  }

  // Loại phòng và phòng vật lý dùng UUID suy ra từ khách sạn để chạy lại không nhân đôi.
  private UUID roomType(UUID hotelId, int maxOccupancy, long basePrice, int rooms) {
    UUID roomTypeId = UUID.nameUUIDFromBytes(("search-it-rt:" + hotelId).getBytes());
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, max_adults, max_occupancy, base_price, is_active)
        VALUES (?, ?, 'Phòng test', ?, ?, ?, true)
        ON CONFLICT (id) DO NOTHING
        """,
        roomTypeId,
        hotelId,
        maxOccupancy,
        maxOccupancy,
        basePrice);
    for (int n = 1; n <= rooms; n++) {
      jdbc.update(
          """
          INSERT INTO rooms (id, hotel_id, room_type_id, room_number, operational_from)
          VALUES (?, ?, ?, ?, '2026-01-01T00:00:00Z')
          ON CONFLICT (id) DO NOTHING
          """,
          UUID.nameUUIDFromBytes(("search-it-room:" + roomTypeId + ":" + n).getBytes()),
          hotelId,
          roomTypeId,
          "S" + n);
    }
    return roomTypeId;
  }

  private void rate(UUID roomTypeId, LocalDate date, long amount) {
    jdbc.update(
        """
        INSERT INTO room_rates (room_type_id, rate_date, amount) VALUES (?, ?, ?)
        ON CONFLICT (room_type_id, rate_date) DO UPDATE SET amount = EXCLUDED.amount
        """,
        roomTypeId,
        date,
        amount);
  }

  private void coverImage(UUID hotelId, UUID mediaId, String status) {
    jdbc.update(
        """
        INSERT INTO media_objects (id, storage_provider, bucket_name, object_key, original_filename,
                                   content_type, size_bytes, purpose, hotel_id, status,
                                   created_by_user_id)
        VALUES (?, 'R2', 'search-it', ?, 'cover.jpg', 'image/jpeg', 10, 'HOTEL_IMAGE', ?,
                CAST(? AS media_status__t), ?)
        ON CONFLICT (id) DO NOTHING
        """,
        mediaId,
        "hotels/" + hotelId + "/images/" + mediaId + ".jpg",
        hotelId,
        status,
        OWNER_ID);
    jdbc.update(
        """
        INSERT INTO hotel_images (hotel_id, media_object_id, display_order, is_cover)
        VALUES (?, ?, 0, true) ON CONFLICT DO NOTHING
        """,
        hotelId,
        mediaId);
  }

  private ResultActions search(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.param("citySlug", CITY_SLUG));
  }

  @Nested
  @DisplayName("Có ngày nhận/trả phòng")
  class DatedSearchTests {

    @Test
    @DisplayName("Hai đêm: chỉ Alpha còn phòng và có giá mọi đêm, có ảnh bìa và tiện ích")
    void shouldReturnOnlyHotelWithStockAndPriceEveryNightWhenTwoNights() throws Exception {
      search(
              get("/api/v1/hotels/search")
                  .param("checkIn", "2027-06-01")
                  .param("checkOut", "2027-06-03")
                  .param("guests", "2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Tìm kiếm khách sạn thành công."))
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content", hasSize(1)))
          .andExpect(jsonPath("$.data.content[0].*", hasSize(11)))
          .andExpect(jsonPath("$.data.content[0].id").value(ALPHA.toString()))
          .andExpect(jsonPath("$.data.content[0].slug").value("search-it-alpha"))
          .andExpect(jsonPath("$.data.content[0].starRating").value(4))
          .andExpect(jsonPath("$.data.content[0].city.slug").value(CITY_SLUG))
          .andExpect(jsonPath("$.data.content[0].city.id").value(CITY_ID.toString()))
          .andExpect(jsonPath("$.data.content[0].coverImage.mediaId").value(ALPHA_COVER.toString()))
          .andExpect(jsonPath("$.data.content[0].minNightlyPrice").value(1_100_000))
          .andExpect(jsonPath("$.data.content[0].totalPrice").value(2_200_000))
          .andExpect(jsonPath("$.data.content[0].currency").value("VND"))
          .andExpect(
              jsonPath("$.data.content[0].amenities[*].code")
                  .value(contains("FREE_PARKING", "SWIMMING_POOL")));
    }

    @Test
    @DisplayName("Một đêm: Bravo và Charlie đủ điều kiện, sắp theo giá tăng dần")
    void shouldIncludeHotelsMissingOnlyLaterNightWhenOneNight() throws Exception {
      search(
              get("/api/v1/hotels/search")
                  .param("checkIn", "2027-06-01")
                  .param("checkOut", "2027-06-02")
                  .param("guests", "2"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.data.content[*].id")
                  .value(contains(CHARLIE.toString(), ALPHA.toString(), BRAVO.toString())))
          .andExpect(jsonPath("$.data.content[0].minNightlyPrice").value(800_000))
          .andExpect(jsonPath("$.data.content[0].coverImage").value(nullValue()))
          .andExpect(jsonPath("$.data.content[2].totalPrice").value(2_000_000));
    }

    @Test
    @DisplayName("Không N+1: số câu SQL không đổi giữa 1 và 3 khách sạn trong kết quả")
    void shouldUseConstantStatementCountWhenMoreHotelsMatch() throws Exception {
      // When
      long oneHotel =
          statementsFor(
              get("/api/v1/hotels/search")
                  .param("checkIn", "2027-06-01")
                  .param("checkOut", "2027-06-02")
                  .param("guests", "2")
                  .param("starRating", "5"),
              1);
      long threeHotels =
          statementsFor(
              get("/api/v1/hotels/search")
                  .param("checkIn", "2027-06-01")
                  .param("checkOut", "2027-06-02")
                  .param("guests", "2"),
              3);

      // 11 truy vấn theo lô: khách sạn, tiện ích, loại phòng, đếm phòng đang phục vụ,
      // phòng vật lý, lịch chặn, giữ/xác nhận phòng, phân công phòng, giá (2), ảnh bìa.
      assertThat(threeHotels)
          .as("Số câu SQL phải không đổi khi kết quả tăng từ 1 lên 3 khách sạn")
          .isEqualTo(oneHotel)
          .isLessThanOrEqualTo(11);
    }

    @Test
    @DisplayName("Lọc khoảng giá theo giá trung bình một đêm")
    void shouldFilterByNightlyPriceWhenPriceRangeGiven() throws Exception {
      search(
              get("/api/v1/hotels/search")
                  .param("checkIn", "2027-06-01")
                  .param("checkOut", "2027-06-02")
                  .param("minPrice", "900000")
                  .param("maxPrice", "1500000"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[*].id").value(contains(ALPHA.toString())));
    }
  }

  @Nested
  @DisplayName("Không có ngày")
  class UndatedSearchTests {

    @Test
    @DisplayName("Chỉ lọc tĩnh: không có DRAFT, SUSPENDED và khách sạn không có phòng phục vụ")
    void shouldApplyStaticFiltersAndExcludeNonActiveHotelsWhenNoDates() throws Exception {
      search(get("/api/v1/hotels/search"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(4))
          .andExpect(
              jsonPath("$.data.content[*].id")
                  .value(
                      contains(
                          DELTA.toString(),
                          ALPHA.toString(),
                          BRAVO.toString(),
                          CHARLIE.toString())))
          .andExpect(jsonPath("$.data.content[0].minNightlyPrice").value(500_000))
          .andExpect(jsonPath("$.data.content[0].totalPrice").value(nullValue()))
          .andExpect(jsonPath("$.data.content[0].coverImage").value(nullValue()))
          .andExpect(jsonPath("$.data.content[3].minNightlyPrice").value(nullValue()));
    }

    @Test
    @DisplayName("Sức chứa: guests=3 chỉ còn khách sạn có loại phòng ≥ 3 khách")
    void shouldFilterByCapacityWhenGuestsGiven() throws Exception {
      search(get("/api/v1/hotels/search").param("guests", "3"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[*].id").value(contains(CHARLIE.toString())));
    }

    @Test
    @DisplayName("Tiện ích: phải có đủ mọi tiện ích được chọn; số sao tối thiểu")
    void shouldRequireEveryAmenityAndMinimumStarsWhenFiltersGiven() throws Exception {
      search(
              get("/api/v1/hotels/search")
                  .param("amenityIds", FREE_PARKING.toString(), SWIMMING_POOL.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[*].id").value(contains(ALPHA.toString())));
      search(get("/api/v1/hotels/search").param("starRating", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[*].id").value(contains(BRAVO.toString())));
    }

    @Test
    @DisplayName("Sắp theo tên và phân trang")
    void shouldSortByNameAndPaginateWhenRequested() throws Exception {
      search(
              get("/api/v1/hotels/search")
                  .param("sort", "name")
                  .param("page", "1")
                  .param("size", "2"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.data.content[*].id")
                  .value(contains(CHARLIE.toString(), DELTA.toString())))
          .andExpect(jsonPath("$.data.page").value(1))
          .andExpect(jsonPath("$.data.totalPages").value(2))
          .andExpect(jsonPath("$.data.last").value(true));
    }
  }

  @Nested
  @DisplayName("Tham số sai")
  class BadRequestTests {

    @Test
    @DisplayName("Trả 400 khi thiếu một trong hai ngày, sort sai hoặc khoảng giá ngược")
    void shouldReturnBadRequestWhenCriteriaInvalid() throws Exception {
      search(get("/api/v1/hotels/search").param("checkOut", "2027-06-02"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
      search(get("/api/v1/hotels/search").param("sort", "cheap"))
          .andExpect(status().isBadRequest());
      search(get("/api/v1/hotels/search").param("minPrice", "2").param("maxPrice", "1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SEARCH_CRITERIA"));
      mvc.perform(
              get("/api/v1/hotels/search")
                  .param("checkIn", "2027-06-01")
                  .param("checkOut", "2027-07-05"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MAX_STAY_EXCEEDED"));
    }

    @Test
    @DisplayName("Không nhầm /search với GET /api/v1/hotels/{hotelId} của module hotel")
    void shouldNotRouteSearchToHotelDetailEndpoint() throws Exception {
      mvc.perform(get("/api/v1/hotels/search")).andExpect(status().isOk());
      mvc.perform(get("/api/v1/hotels/" + ALPHA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(ALPHA.toString()));
    }
  }
}

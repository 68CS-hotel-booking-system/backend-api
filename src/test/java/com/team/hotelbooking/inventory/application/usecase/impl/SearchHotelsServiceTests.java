package com.team.hotelbooking.inventory.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.inventory.application.command.SearchHotelsQuery;
import com.team.hotelbooking.inventory.application.port.HotelCoverImagePort;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort.CatalogRoomType;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort.City;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort.SearchableHotel;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.StayPricingPort;
import com.team.hotelbooking.inventory.application.port.StayPricingPort.RoomTypeStayPrice;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ tìm kiếm khách sạn")
class SearchHotelsServiceTests {

  private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");
  private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
  private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 3);
  private static final City DA_NANG =
      new City(UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3"), "Đà Nẵng", "da-nang");

  private static final UUID HOTEL_OK = id("a1");
  private static final UUID HOTEL_FULL = id("a2");
  private static final UUID HOTEL_NO_PRICE = id("a3");
  private static final UUID HOTEL_SMALL = id("a4");
  private static final UUID RT_OK_CHEAP = id("b1");
  private static final UUID RT_OK_DEAR = id("b2");
  private static final UUID RT_FULL = id("b3");
  private static final UUID RT_NO_PRICE = id("b4");
  private static final UUID RT_SMALL = id("b5");
  private static final UUID COVER_ID = id("c1");

  @Mock private HotelSearchCatalogPort catalog;
  @Mock private InventoryQueryPort inventory;
  @Mock private StayPricingPort pricing;
  @Mock private HotelCoverImagePort covers;

  private SearchHotelsService service;

  @BeforeEach
  void setUp() {
    service =
        new SearchHotelsService(
            catalog, inventory, pricing, covers, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldReturnEmptyPageWhenOffsetExceedsIntegerRange() {
    given(catalog.findSearchableHotels(any(), any(), any(), any()))
        .willReturn(List.of(hotel(HOTEL_OK, "Hotel", 3)));
    given(catalog.findActiveRoomTypes(anyCollection()))
        .willReturn(List.of(new CatalogRoomType(RT_OK_CHEAP, HOTEL_OK, 2, 100L)));
    given(catalog.countOperationalRooms(anyCollection(), any()))
        .willReturn(Map.of(RT_OK_CHEAP, 1L));
    var result = service.execute(query(null, null, 1, null, null, null, Integer.MAX_VALUE, 20));
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(1);
  }

  private static UUID id(String suffix) {
    return UUID.fromString("00000000-0000-4000-8000-0000000000" + suffix);
  }

  private static SearchableHotel hotel(UUID id, String name, int stars) {
    return new SearchableHotel(
        id, name, "slug-" + name, "Địa chỉ", (short) stars, DA_NANG, List.of());
  }

  private static SearchHotelsQuery query(
      LocalDate checkIn,
      LocalDate checkOut,
      Integer guests,
      Long minPrice,
      Long maxPrice,
      SearchHotelsQuery.Sort sort,
      int page,
      int size) {
    return new SearchHotelsQuery(
        null,
        "Da-Nang ",
        checkIn,
        checkOut,
        guests,
        minPrice,
        maxPrice,
        null,
        Set.of(),
        sort,
        new PageQuery(page, size));
  }

  private void givenCatalogOfFourHotels() {
    given(catalog.findSearchableHotels(null, "da-nang", null, Set.of()))
        .willReturn(
            List.of(
                hotel(HOTEL_OK, "Biển Xanh", 4),
                hotel(HOTEL_FULL, "Hết Phòng", 5),
                hotel(HOTEL_NO_PRICE, "Chưa Giá", 3),
                hotel(HOTEL_SMALL, "Phòng Nhỏ", 3)));
    given(catalog.findActiveRoomTypes(List.of(HOTEL_OK, HOTEL_FULL, HOTEL_NO_PRICE, HOTEL_SMALL)))
        .willReturn(
            List.of(
                new CatalogRoomType(RT_OK_CHEAP, HOTEL_OK, 2, 900_000L),
                new CatalogRoomType(RT_OK_DEAR, HOTEL_OK, 3, 1_500_000L),
                new CatalogRoomType(RT_FULL, HOTEL_FULL, 2, 1_000_000L),
                new CatalogRoomType(RT_NO_PRICE, HOTEL_NO_PRICE, 2, 0L),
                new CatalogRoomType(RT_SMALL, HOTEL_SMALL, 1, 500_000L)));
  }

  @Nested
  @DisplayName("Có ngày nhận/trả phòng")
  class DatedSearchTests {

    @Test
    @DisplayName("Chỉ giữ khách sạn còn tồn mọi đêm, có giá mọi đêm và đủ sức chứa")
    void shouldKeepOnlyHotelWithStockPriceAndCapacityWhenDatesGiven() {
      // Given
      givenCatalogOfFourHotels();
      given(
              catalog.countOperationalRooms(
                  List.of(RT_OK_CHEAP, RT_OK_DEAR, RT_FULL, RT_NO_PRICE), NOW))
          .willReturn(Map.of(RT_OK_CHEAP, 3L, RT_OK_DEAR, 1L, RT_FULL, 2L, RT_NO_PRICE, 2L));
      given(inventory.calculateMinAvailableQuantities(anyMap(), any(), any()))
          .willReturn(Map.of(RT_OK_CHEAP, 2, RT_OK_DEAR, 1, RT_FULL, 0, RT_NO_PRICE, 2));
      given(
              pricing.findStayPrices(
                  List.of(RT_OK_CHEAP, RT_OK_DEAR, RT_NO_PRICE), CHECK_IN, CHECK_OUT))
          .willReturn(
              Map.of(
                  RT_OK_CHEAP, new RoomTypeStayPrice(RT_OK_CHEAP, true, 1_900_000L),
                  RT_OK_DEAR, new RoomTypeStayPrice(RT_OK_DEAR, true, 3_000_000L),
                  RT_NO_PRICE, new RoomTypeStayPrice(RT_NO_PRICE, false, 0L)));
      given(covers.findCoverImageIds(List.of(HOTEL_OK))).willReturn(Map.of(HOTEL_OK, COVER_ID));

      // When
      PageResult<HotelSearchItemResult> result =
          service.execute(query(CHECK_IN, CHECK_OUT, 2, null, null, null, 0, 20));

      // Then
      assertThat(result.totalElements()).isEqualTo(1);
      HotelSearchItemResult item = result.content().get(0);
      assertThat(item.id()).isEqualTo(HOTEL_OK);
      assertThat(item.minNightlyPrice()).isEqualTo(950_000L);
      assertThat(item.totalPrice()).isEqualTo(1_900_000L);
      assertThat(item.currency()).isEqualTo("VND");
      assertThat(item.coverImage().mediaId()).isEqualTo(COVER_ID);
      assertThat(item.city().slug()).isEqualTo("da-nang");
    }

    @Test
    @DisplayName("Lọc giá theo giá trung bình một đêm, loại phòng ngoài khoảng bị bỏ")
    void shouldUseRoomTypeInsidePriceRangeWhenPriceFilterGiven() {
      // Given
      givenCatalogOfFourHotels();
      given(catalog.countOperationalRooms(anyCollection(), any()))
          .willReturn(Map.of(RT_OK_CHEAP, 3L, RT_OK_DEAR, 1L));
      given(inventory.calculateMinAvailableQuantities(anyMap(), any(), any()))
          .willReturn(Map.of(RT_OK_CHEAP, 2, RT_OK_DEAR, 1));
      given(pricing.findStayPrices(anyCollection(), any(), any()))
          .willReturn(
              Map.of(
                  RT_OK_CHEAP, new RoomTypeStayPrice(RT_OK_CHEAP, true, 1_900_000L),
                  RT_OK_DEAR, new RoomTypeStayPrice(RT_OK_DEAR, true, 3_000_000L)));
      given(covers.findCoverImageIds(List.of(HOTEL_OK))).willReturn(Map.of());

      // When
      HotelSearchItemResult item =
          service
              .execute(query(CHECK_IN, CHECK_OUT, 1, 1_000_000L, 2_000_000L, null, 0, 20))
              .content()
              .get(0);

      // Then: chỉ loại phòng 1.500.000/đêm nằm trong khoảng; chưa có ảnh bìa → null
      assertThat(item.minNightlyPrice()).isEqualTo(1_500_000L);
      assertThat(item.totalPrice()).isEqualTo(3_000_000L);
      assertThat(item.coverImage()).isNull();
    }

    @Test
    @DisplayName("Không gọi tồn và giá khi không có loại phòng nào có phòng đang phục vụ")
    void shouldSkipInventoryAndPricingWhenNoServingRoomType() {
      // Given
      givenCatalogOfFourHotels();
      given(catalog.countOperationalRooms(anyCollection(), any())).willReturn(Map.of());

      // When
      PageResult<HotelSearchItemResult> result =
          service.execute(query(CHECK_IN, CHECK_OUT, 1, null, null, null, 0, 20));

      // Then
      assertThat(result.content()).isEmpty();
      assertThat(result.totalElements()).isZero();
      then(inventory).shouldHaveNoInteractions();
      then(pricing).shouldHaveNoInteractions();
      then(covers).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("Không có ngày")
  class UndatedSearchTests {

    @Test
    @DisplayName("Chỉ lọc tĩnh, giá tham khảo là base_price > 0, sắp theo giá với giá null cuối")
    void shouldApplyStaticFiltersOnlyWhenNoDates() {
      // Given
      givenCatalogOfFourHotels();
      given(catalog.countOperationalRooms(anyCollection(), any()))
          .willReturn(Map.of(RT_OK_CHEAP, 3L, RT_FULL, 2L, RT_NO_PRICE, 2L, RT_SMALL, 1L));
      given(covers.findCoverImageIds(anyCollection())).willReturn(Map.of());

      // When
      PageResult<HotelSearchItemResult> result =
          service.execute(query(null, null, null, null, null, null, 0, 20));

      // Then
      assertThat(result.content())
          .extracting(HotelSearchItemResult::id)
          .containsExactly(HOTEL_SMALL, HOTEL_OK, HOTEL_FULL, HOTEL_NO_PRICE);
      assertThat(result.content().get(0).minNightlyPrice()).isEqualTo(500_000L);
      assertThat(result.content().get(3).minNightlyPrice()).isNull();
      assertThat(result.content().get(0).totalPrice()).isNull();
      then(inventory).shouldHaveNoInteractions();
      then(pricing).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Sắp theo số sao giảm dần và phân trang đúng tổng số")
    void shouldSortByRatingAndPaginateWhenRatingSortRequested() {
      // Given
      givenCatalogOfFourHotels();
      given(catalog.countOperationalRooms(anyCollection(), any()))
          .willReturn(Map.of(RT_OK_CHEAP, 3L, RT_FULL, 2L, RT_NO_PRICE, 2L, RT_SMALL, 1L));
      given(covers.findCoverImageIds(List.of(HOTEL_OK))).willReturn(Map.of());

      // When
      PageResult<HotelSearchItemResult> result =
          service.execute(query(null, null, 1, null, null, SearchHotelsQuery.Sort.RATING, 1, 1));

      // Then: 5★ Hết Phòng, 4★ Biển Xanh, 3★ Phòng Nhỏ (rẻ hơn), 3★ Chưa Giá
      assertThat(result.content()).extracting(HotelSearchItemResult::id).containsExactly(HOTEL_OK);
      assertThat(result.totalElements()).isEqualTo(4);
      assertThat(result.totalPages()).isEqualTo(4);
      assertThat(result.first()).isFalse();
      assertThat(result.last()).isFalse();
    }
  }

  @Nested
  @DisplayName("Kiểm tra điều kiện")
  class ValidationTests {

    @Test
    @DisplayName("Từ chối khi chỉ có một trong hai ngày")
    void shouldRejectWhenOnlyOneDateGiven() {
      assertThatThrownBy(() -> service.execute(query(CHECK_IN, null, 1, null, null, null, 0, 20)))
          .isInstanceOf(InventoryException.class)
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.INVALID_DATE_RANGE));
      then(catalog).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Từ chối quá 30 đêm và ngày nhận phòng trong quá khứ")
    void shouldRejectWhenStayTooLongOrCheckInInPast() {
      assertThatThrownBy(
              () ->
                  service.execute(
                      query(CHECK_IN, CHECK_IN.plusDays(31), 1, null, null, null, 0, 20)))
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.MAX_STAY_EXCEEDED));
      assertThatThrownBy(
              () ->
                  service.execute(
                      query(LocalDate.of(2026, 11, 9), CHECK_OUT, 1, null, null, null, 0, 20)))
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    @DisplayName("Từ chối giá tối thiểu lớn hơn giá tối đa và truyền cả cityId lẫn citySlug")
    void shouldRejectInvalidPriceRangeAndDuplicatedCity() {
      assertThatThrownBy(
              () -> service.execute(query(null, null, 1, 2_000_000L, 1_000_000L, null, 0, 20)))
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.INVALID_SEARCH_CRITERIA));
      assertThatThrownBy(
              () ->
                  service.execute(
                      new SearchHotelsQuery(
                          DA_NANG.id(),
                          "da-nang",
                          null,
                          null,
                          1,
                          null,
                          null,
                          null,
                          Set.of(),
                          null,
                          new PageQuery(0, 20))))
          .satisfies(
              ex ->
                  assertThat(((InventoryException) ex).code())
                      .isEqualTo(InventoryErrorCode.INVALID_SEARCH_CRITERIA));
      then(catalog).should(never()).findSearchableHotels(any(), any(), any(), any());
    }
  }
}

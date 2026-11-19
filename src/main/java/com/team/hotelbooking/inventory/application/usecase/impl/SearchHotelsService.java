package com.team.hotelbooking.inventory.application.usecase.impl;

import com.team.hotelbooking.inventory.application.command.SearchHotelsQuery;
import com.team.hotelbooking.inventory.application.port.HotelCoverImagePort;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort.CatalogRoomType;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort.SearchableHotel;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.StayPricingPort;
import com.team.hotelbooking.inventory.application.port.StayPricingPort.RoomTypeStayPrice;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult.AmenityResult;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult.CityResult;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult.CoverImageResult;
import com.team.hotelbooking.inventory.application.usecase.SearchHotelsUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tìm kiếm khách sạn công khai (HT-05, IV-13, PR-02). Một khách sạn xuất hiện khi đang ACTIVE và có
 * ít nhất một loại phòng đang bán thỏa mọi điều kiện:
 *
 * <ul>
 *   <li>Sức chứa (max_occupancy) ≥ số khách, có phòng vật lý đang phục vụ.
 *   <li>Có ngày: còn ≥ 1 phòng MỌI đêm (IV-13) và có giá hợp lệ MỌI đêm theo pricing (PR-02).
 *   <li>Lọc giá theo giá trung bình một đêm (có ngày) hoặc base_price (không có ngày).
 * </ul>
 *
 * <p>Số truy vấn không phụ thuộc số khách sạn: khách sạn, tiện ích, loại phòng, phòng đang phục vụ,
 * tồn (4), giá (2) và ảnh bìa của trang hiện tại. Lọc/sắp xếp/phân trang làm trong bộ nhớ sau khi
 * lọc tĩnh ở DB.
 */
public final class SearchHotelsService implements SearchHotelsUseCase {

  static final int MAX_STAY_NIGHTS = 30;
  static final String CURRENCY = "VND";

  private final HotelSearchCatalogPort catalog;
  private final InventoryQueryPort inventory;
  private final StayPricingPort pricing;
  private final HotelCoverImagePort coverImages;
  private final Clock clock;

  public SearchHotelsService(
      HotelSearchCatalogPort catalog,
      InventoryQueryPort inventory,
      StayPricingPort pricing,
      HotelCoverImagePort coverImages,
      Clock clock) {
    this.catalog = catalog;
    this.inventory = inventory;
    this.pricing = pricing;
    this.coverImages = coverImages;
    this.clock = clock;
  }

  @Override
  public PageResult<HotelSearchItemResult> execute(SearchHotelsQuery query) {
    validate(query);
    boolean dated = query.checkIn() != null;
    int guests = query.guests() == null ? 1 : query.guests();
    String citySlug =
        query.citySlug() == null ? null : query.citySlug().trim().toLowerCase(Locale.ROOT);

    List<SearchableHotel> hotels =
        catalog.findSearchableHotels(
            query.cityId(), citySlug, query.minStarRating(), query.amenityIds());
    if (hotels.isEmpty()) {
      return page(List.of(), query.pagination());
    }

    // Loại phòng đủ sức chứa và có phòng đang phục vụ.
    List<CatalogRoomType> roomTypes =
        catalog.findActiveRoomTypes(hotels.stream().map(SearchableHotel::id).toList()).stream()
            .filter(roomType -> roomType.maxOccupancy() >= guests)
            .toList();
    Map<UUID, Long> operationalRooms =
        catalog.countOperationalRooms(
            roomTypes.stream().map(CatalogRoomType::id).toList(), clock.instant());
    List<CatalogRoomType> serving =
        roomTypes.stream()
            .filter(roomType -> operationalRooms.getOrDefault(roomType.id(), 0L) > 0)
            .toList();

    Map<UUID, Offer> bestOfferByHotel =
        dated
            ? datedOffers(serving, operationalRooms, query)
            : undatedOffers(serving, query.minPrice(), query.maxPrice());

    List<Candidate> candidates = new ArrayList<>();
    for (SearchableHotel hotel : hotels) {
      Offer offer = bestOfferByHotel.get(hotel.id());
      if (offer != null) {
        candidates.add(new Candidate(hotel, offer));
      }
    }
    candidates.sort(comparator(query.sort()));

    PageQuery pagination = query.pagination();
    long offset = (long) pagination.page() * pagination.size();
    int from = (int) Math.min(candidates.size(), offset);
    int to = (int) Math.min(candidates.size(), (long) from + pagination.size());
    List<Candidate> pageItems = candidates.subList(from, to);

    // Ảnh bìa chỉ lấy cho khách sạn của trang hiện tại; tất cả đều ACTIVE (MD-06).
    Map<UUID, UUID> covers =
        pageItems.isEmpty()
            ? Map.of()
            : coverImages.findCoverImageIds(pageItems.stream().map(c -> c.hotel().id()).toList());

    List<HotelSearchItemResult> content =
        pageItems.stream().map(candidate -> toResult(candidate, covers)).toList();
    return new PageResult<>(
        content,
        pagination.page(),
        pagination.size(),
        candidates.size(),
        totalPages(candidates.size(), pagination.size()),
        pagination.page() == 0,
        to >= candidates.size());
  }

  private Map<UUID, Offer> datedOffers(
      List<CatalogRoomType> serving, Map<UUID, Long> operationalRooms, SearchHotelsQuery query) {
    if (serving.isEmpty()) {
      return Map.of();
    }
    long nights = ChronoUnit.DAYS.between(query.checkIn(), query.checkOut());

    Map<UUID, Long> servingRooms = new HashMap<>();
    serving.forEach(
        roomType -> servingRooms.put(roomType.id(), operationalRooms.get(roomType.id())));
    Map<UUID, Integer> available =
        inventory.calculateMinAvailableQuantities(servingRooms, query.checkIn(), query.checkOut());

    List<CatalogRoomType> inStock =
        serving.stream().filter(roomType -> available.getOrDefault(roomType.id(), 0) >= 1).toList();
    if (inStock.isEmpty()) {
      return Map.of();
    }
    Map<UUID, RoomTypeStayPrice> prices =
        pricing.findStayPrices(
            inStock.stream().map(CatalogRoomType::id).toList(), query.checkIn(), query.checkOut());

    Map<UUID, Offer> best = new HashMap<>();
    for (CatalogRoomType roomType : inStock) {
      RoomTypeStayPrice price = prices.get(roomType.id());
      if (price == null || !price.sellable()) {
        continue;
      }
      long nightly = price.totalAmount() / nights;
      if (withinPrice(nightly, query.minPrice(), query.maxPrice())) {
        best.merge(roomType.hotelId(), new Offer(nightly, price.totalAmount()), Offer::cheaper);
      }
    }
    return best;
  }

  // Không có ngày: chỉ lọc tĩnh; giá tham khảo là base_price > 0, có thể null.
  private static Map<UUID, Offer> undatedOffers(
      List<CatalogRoomType> serving, Long minPrice, Long maxPrice) {
    boolean priceFiltered = minPrice != null || maxPrice != null;
    Map<UUID, Offer> best = new HashMap<>();
    for (CatalogRoomType roomType : serving) {
      Long basePrice = roomType.basePrice() > 0 ? roomType.basePrice() : null;
      if (priceFiltered && (basePrice == null || !withinPrice(basePrice, minPrice, maxPrice))) {
        continue;
      }
      best.merge(roomType.hotelId(), new Offer(basePrice, null), Offer::cheaper);
    }
    return best;
  }

  private void validate(SearchHotelsQuery query) {
    if (query == null || query.pagination() == null) {
      throw InventoryException.invalidSearchCriteria("Phân trang là bắt buộc.");
    }
    if (query.cityId() != null && query.citySlug() != null) {
      throw InventoryException.invalidSearchCriteria("Chỉ truyền một trong cityId hoặc citySlug.");
    }
    if (query.citySlug() != null && query.citySlug().isBlank()) {
      throw InventoryException.invalidSearchCriteria("citySlug không được để trống.");
    }
    if (query.guests() != null && query.guests() < 1) {
      throw InventoryException.invalidSearchCriteria("Số khách tối thiểu là 1.");
    }
    if ((query.minPrice() != null && query.minPrice() < 0)
        || (query.maxPrice() != null && query.maxPrice() < 0)) {
      throw InventoryException.invalidSearchCriteria("Khoảng giá không được âm.");
    }
    if (query.minPrice() != null
        && query.maxPrice() != null
        && query.minPrice() > query.maxPrice()) {
      throw InventoryException.invalidSearchCriteria(
          "Giá tối thiểu không được lớn hơn giá tối đa.");
    }
    if (query.minStarRating() != null && (query.minStarRating() < 1 || query.minStarRating() > 5)) {
      throw InventoryException.invalidSearchCriteria("Số sao phải từ 1 đến 5.");
    }
    validateDates(query.checkIn(), query.checkOut());
  }

  // Cùng giới hạn với tra phòng trống và báo giá: không quá khứ, tối thiểu 1 đêm, tối đa 30 đêm.
  private void validateDates(LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null && checkOut == null) {
      return;
    }
    if (checkIn == null || checkOut == null) {
      throw InventoryException.invalidDateRange(
          "Ngày nhận và trả phòng phải được truyền cùng nhau.");
    }
    if (checkIn.isBefore(BusinessTime.today(clock))) {
      throw InventoryException.invalidDateRange("Ngày nhận phòng không thể ở trong quá khứ.");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw InventoryException.invalidDateRange(
          "Ngày trả phòng phải sau ngày nhận phòng tối thiểu 1 đêm.");
    }
    if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_STAY_NIGHTS) {
      throw InventoryException.maxStayExceeded("Thời gian lưu trú tối đa là 30 đêm.");
    }
  }

  private static boolean withinPrice(long price, Long minPrice, Long maxPrice) {
    return (minPrice == null || price >= minPrice) && (maxPrice == null || price <= maxPrice);
  }

  private static Comparator<Candidate> comparator(SearchHotelsQuery.Sort sort) {
    Comparator<Candidate> byPrice =
        Comparator.comparing(
            (Candidate c) -> c.offer().nightlyPrice(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    Comparator<Candidate> byName =
        Comparator.comparing((Candidate c) -> c.hotel().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(c -> c.hotel().id());
    Comparator<Candidate> byRating =
        Comparator.comparing(
            (Candidate c) -> c.hotel().starRating(),
            Comparator.nullsLast(Comparator.<Short>reverseOrder()));

    return switch (sort == null ? SearchHotelsQuery.Sort.PRICE : sort) {
      case PRICE -> byPrice.thenComparing(byName);
      case RATING -> byRating.thenComparing(byPrice).thenComparing(byName);
      case NAME -> byName;
    };
  }

  private static HotelSearchItemResult toResult(Candidate candidate, Map<UUID, UUID> covers) {
    SearchableHotel hotel = candidate.hotel();
    UUID coverId = covers.get(hotel.id());
    return new HotelSearchItemResult(
        hotel.id(),
        hotel.name(),
        hotel.slug(),
        hotel.address(),
        hotel.starRating(),
        new CityResult(hotel.city().id(), hotel.city().name(), hotel.city().slug()),
        coverId == null ? null : new CoverImageResult(coverId),
        candidate.offer().nightlyPrice(),
        candidate.offer().totalPrice(),
        CURRENCY,
        hotel.amenities().stream()
            .map(amenity -> new AmenityResult(amenity.id(), amenity.code(), amenity.name()))
            .toList());
  }

  private static PageResult<HotelSearchItemResult> page(
      List<HotelSearchItemResult> content, PageQuery pagination) {
    return new PageResult<>(
        content, pagination.page(), pagination.size(), 0, 0, pagination.page() == 0, true);
  }

  private static int totalPages(int totalElements, int size) {
    return (totalElements + size - 1) / size;
  }

  private record Candidate(SearchableHotel hotel, Offer offer) {}

  // nightlyPrice null: chưa có giá tham khảo (không có ngày và base_price = 0).
  private record Offer(Long nightlyPrice, Long totalPrice) {

    static Offer cheaper(Offer left, Offer right) {
      if (left.nightlyPrice() == null) {
        return right;
      }
      if (right.nightlyPrice() == null) {
        return left;
      }
      return right.nightlyPrice() < left.nightlyPrice() ? right : left;
    }
  }
}

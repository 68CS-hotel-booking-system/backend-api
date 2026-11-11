package com.team.hotelbooking.inventory.api.dto.request;

import com.team.hotelbooking.inventory.application.command.SearchHotelsQuery;
import com.team.hotelbooking.shared.pagination.PageQuery;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record SearchHotelsRequest(
    @Parameter(description = "ID thành phố; không truyền cùng citySlug") UUID cityId,
    @Parameter(description = "Slug thành phố, ví dụ da-nang; không truyền cùng cityId")
        @Size(max = 150, message = "Slug thành phố tối đa 150 ký tự.")
        String citySlug,
    @Parameter(description = "Ngày nhận phòng (YYYY-MM-DD); truyền cùng checkOut")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkIn,
    @Parameter(description = "Ngày trả phòng (YYYY-MM-DD), sau checkIn, tối đa 30 đêm")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate checkOut,
    @Parameter(description = "Số khách trong một phòng (mặc định 1)")
        @Min(value = 1, message = "Số khách tối thiểu là 1.")
        @Max(value = 50, message = "Số khách tối đa là 50.")
        Integer guests,
    @Parameter(description = "Giá tối thiểu một đêm (VND)")
        @Min(value = 0, message = "Giá tối thiểu không được âm.")
        Long minPrice,
    @Parameter(description = "Giá tối đa một đêm (VND)")
        @Min(value = 0, message = "Giá tối đa không được âm.")
        Long maxPrice,
    @Parameter(description = "Số sao tối thiểu (1–5)")
        @Min(value = 1, message = "Số sao phải từ 1 đến 5.")
        @Max(value = 5, message = "Số sao phải từ 1 đến 5.")
        Integer starRating,
    @Parameter(description = "Khách sạn phải có đủ mọi tiện ích (loại HOTEL) trong danh sách")
        @Size(max = 20, message = "Tối đa 20 tiện ích.")
        List<UUID> amenityIds,
    @Parameter(description = "Sắp xếp: price (mặc định, tăng dần), rating (giảm dần), name")
        @Pattern(regexp = "price|rating|name", message = "sort chỉ nhận price, rating hoặc name.")
        String sort,
    @Parameter(description = "Số trang, bắt đầu từ 0") Integer page,
    @Parameter(description = "Kích thước trang") Integer size) {

  public SearchHotelsQuery toQuery(PageQuery pagination) {
    return new SearchHotelsQuery(
        cityId,
        citySlug,
        checkIn,
        checkOut,
        guests,
        minPrice,
        maxPrice,
        starRating,
        amenityIds == null ? Set.of() : Set.copyOf(amenityIds),
        sort == null ? null : SearchHotelsQuery.Sort.valueOf(sort.toUpperCase(Locale.ROOT)),
        pagination);
  }
}

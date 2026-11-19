package com.team.hotelbooking.shared.web;

import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public PageResponse {
    content = List.copyOf(content);
  }

  public static <T> PageResponse<T> from(PageResult<T> source) {
    return new PageResponse<>(
        source.content(),
        source.page(),
        source.size(),
        source.totalElements(),
        source.totalPages(),
        source.first(),
        source.last());
  }
}

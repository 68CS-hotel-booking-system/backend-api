package com.team.hotelbooking.shared.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public PageResult {
    content = List.copyOf(content);
  }

  public <R> PageResult<R> map(Function<T, R> mapper) {
    List<R> mappedContent = content.stream().map(mapper).toList();

    return new PageResult<>(mappedContent, page, size, totalElements, totalPages, first, last);
  }
}

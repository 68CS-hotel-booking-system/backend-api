package com.team.hotelbooking.shared.persistence;

import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class PageResultMapper {

  private PageResultMapper() {}

  public static <E, T> PageResult<T> from(Page<E> page, Function<E, T> mapper) {
    return new PageResult<>(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }
}

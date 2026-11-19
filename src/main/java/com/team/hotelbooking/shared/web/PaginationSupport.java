package com.team.hotelbooking.shared.web;

import com.team.hotelbooking.shared.config.PaginationProperties;
import com.team.hotelbooking.shared.pagination.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PaginationSupport {

  private final PaginationProperties properties;

  public PageQuery resolve(Integer page, Integer size) {
    int resolvedPage = page == null ? 0 : page;
    int resolvedSize = size == null ? properties.defaultSize() : size;

    if (resolvedPage < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số trang không được âm.");
    }

    if (resolvedSize < 1 || resolvedSize > properties.maxSize()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Kích thước trang phải từ 1 đến " + properties.maxSize() + ".");
    }

    return new PageQuery(resolvedPage, resolvedSize);
  }
}

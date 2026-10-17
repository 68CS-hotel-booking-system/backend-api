package com.team.hotelbooking.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable pagination response with zero-based page numbers. Map entities to DTOs first. */
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

  public static <T> PageResponse<T> from(Page<T> source) {
    return new PageResponse<>(
        source.getContent(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages(),
        source.isFirst(),
        source.isLast());
  }
}

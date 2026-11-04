package com.team.hotelbooking.shared.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResultMapperTests {

  @Test
  void convertsSpringPageAndMapsContent() {
    PageImpl<Integer> page = new PageImpl<>(List.of(3, 4), PageRequest.of(1, 2), 7);

    PageResult<String> result = PageResultMapper.from(page, id -> "hotel-" + id);

    assertEquals(List.of("hotel-3", "hotel-4"), result.content());
    assertEquals(1, result.page());
    assertEquals(2, result.size());
    assertEquals(7, result.totalElements());
    assertEquals(4, result.totalPages());
    assertFalse(result.first());
    assertFalse(result.last());
  }
}

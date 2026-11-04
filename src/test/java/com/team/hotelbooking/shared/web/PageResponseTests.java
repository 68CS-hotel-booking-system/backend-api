package com.team.hotelbooking.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTests {

  @Test
  void copiesPageResultMetadata() {
    PageResult<String> source =
        new PageResult<>(List.of("hotel-3", "hotel-4"), 1, 2, 7, 4, false, false);

    PageResponse<String> response = PageResponse.from(source);

    assertEquals(List.of("hotel-3", "hotel-4"), response.content());
    assertEquals(1, response.page());
    assertEquals(2, response.size());
    assertEquals(7, response.totalElements());
    assertEquals(4, response.totalPages());
    assertFalse(response.first());
    assertFalse(response.last());
  }

  @Test
  void supportsEmptyPages() {
    PageResult<String> source = new PageResult<>(List.of(), 0, 10, 0, 0, true, true);

    PageResponse<String> response = PageResponse.from(source);

    assertTrue(response.content().isEmpty());
    assertEquals(10, response.size());
    assertEquals(0, response.totalElements());
    assertEquals(0, response.totalPages());
    assertTrue(response.first());
    assertTrue(response.last());
  }

  @Test
  void responseContentDoesNotChangeWhenSourceListChanges() {
    List<String> content = new ArrayList<>(List.of("hotel-1"));
    PageResponse<String> response = new PageResponse<>(content, 0, 10, 1, 1, true, true);

    content.clear();

    assertEquals(List.of("hotel-1"), response.content());
  }
}

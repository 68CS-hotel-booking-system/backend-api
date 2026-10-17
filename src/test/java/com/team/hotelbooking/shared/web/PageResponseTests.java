package com.team.hotelbooking.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTests {

  @Test
  void preservesPaginationMetadataAfterMappingToDtos() {
    Page<Integer> source = new PageImpl<>(List.of(3, 4), PageRequest.of(1, 2), 7);

    PageResponse<String> response = PageResponse.from(source.map(id -> "hotel-" + id));

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
    PageResponse<String> response = PageResponse.from(Page.empty(PageRequest.of(0, 10)));

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

package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;

public record ListAdminUsersRequest(
    @Size(max = 255) String search,
    @Pattern(
            regexp = "active|inactive|pending|banned",
            message = "Trạng thái phải là active, inactive, pending hoặc banned.")
        String status,
    @Size(max = 50) String role,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
    @Pattern(
            regexp = "(createdAt|fullName|email),(asc|desc)",
            message = "Sắp xếp theo createdAt, fullName hoặc email với asc hoặc desc.")
        String sort,
    Integer page,
    Integer size) {

  public ListAdminUsersRequest {
    if (sort == null || sort.isEmpty()) {
      sort = "createdAt,desc";
    }
  }
}

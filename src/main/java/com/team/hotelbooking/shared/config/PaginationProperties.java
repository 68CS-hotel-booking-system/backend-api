package com.team.hotelbooking.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(int defaultSize, int maxSize) {

  public PaginationProperties {
    if (defaultSize < 1 || maxSize < 1 || defaultSize > maxSize) {
      throw new IllegalArgumentException(
          "Pagination configuration requires 1 <= default-size <= max-size.");
    }
  }
}

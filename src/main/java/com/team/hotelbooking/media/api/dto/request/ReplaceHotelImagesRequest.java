// ReplaceHotelImagesRequest.java
package com.team.hotelbooking.media.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReplaceHotelImagesRequest(
    @NotNull List<@NotNull @Valid Image> images, @NotNull @Min(0) Long expectedVersion) {

  public record Image(
      @NotNull UUID mediaId,
      UUID albumId,
      @NotNull @Min(0) Integer sortOrder,
      @NotNull Boolean cover) {}
}

// UpdateHotelImageAlbumRequest.java
package com.team.hotelbooking.media.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateHotelImageAlbumRequest(
    @Size(max = 100) String name,
    @Size(max = 2000) String description,
    @NotNull @Min(0) Long expectedVersion) {}

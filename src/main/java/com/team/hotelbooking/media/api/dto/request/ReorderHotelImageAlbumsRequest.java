// ReorderHotelImageAlbumsRequest.java
package com.team.hotelbooking.media.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderHotelImageAlbumsRequest(
    @NotNull List<@NotNull UUID> albumIds, @NotNull @Min(0) Long expectedVersion) {}

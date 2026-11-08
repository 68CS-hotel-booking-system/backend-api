// UploadHotelImageRequest.java
package com.team.hotelbooking.media.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record UploadHotelImageRequest(
    @NotNull MultipartFile file,
    @NotBlank @Pattern(regexp = "HOTEL_IMAGE", message = "Luồng này nhận purpose HOTEL_IMAGE.")
        String purpose,
    @NotNull UUID hotelId) {}

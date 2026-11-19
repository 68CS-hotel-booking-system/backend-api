package com.team.hotelbooking.media.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AttachRoomTypeImageRequest(@NotNull UUID mediaId) {}

package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record AttachRoomTypeImageCommand(
    UUID actorId, UUID hotelId, UUID roomTypeId, UUID mediaId) {}

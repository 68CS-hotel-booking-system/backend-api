package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record UpdateRoomCommand(
    UUID hotelId, UUID roomId, String roomNumber, Short floor, String status, Boolean active) {}

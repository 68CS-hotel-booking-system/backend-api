package com.team.hotelbooking.hotel.domain.model;

import java.util.UUID;

public record City(UUID id, String countryCode, String name, String slug) {}

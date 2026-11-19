package com.team.hotelbooking.hotel.application.command;

import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListHotelReviewsQuery(
    UUID actorId, HotelReviewRequest.Status status, UUID hotelId, PageQuery pagination) {}

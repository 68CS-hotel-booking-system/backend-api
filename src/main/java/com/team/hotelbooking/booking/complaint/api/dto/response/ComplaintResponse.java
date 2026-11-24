package com.team.hotelbooking.booking.complaint.api.dto.response;

import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ComplaintResponse(
    UUID id,
    UUID bookingId,
    UUID hotelId,
    String category,
    String content,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version,
    List<ComplaintActionResponse> history) {

  public static ComplaintResponse from(ComplaintResult result) {
    return new ComplaintResponse(
        result.id(),
        result.bookingId(),
        result.hotelId(),
        result.category(),
        result.content(),
        result.status(),
        result.createdAt(),
        result.updatedAt(),
        result.version(),
        result.history().stream().map(ComplaintActionResponse::from).toList());
  }

  public static PageResponse<ComplaintResponse> from(PageResult<ComplaintResult> result) {
    return new PageResponse<>(
        result.content().stream().map(ComplaintResponse::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.first(),
        result.last());
  }
}

package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelReviewRequestJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelReviewRequestJpaRepository
    extends JpaRepository<HotelReviewRequestJpaEntity, UUID> {
  Optional<HotelReviewRequestJpaEntity> findByHotelIdAndStatus(
      UUID hotelId, HotelReviewRequest.Status status);

  @Query("select r.hotelId from HotelReviewRequestJpaEntity r where r.id = :id")
  Optional<UUID> findHotelId(@Param("id") UUID id);

  // Projection bỏ cột JSON snapshot; danh sách không nạp toàn bộ hồ sơ.
  @Query(
      """
      select r.id as id, r.hotelId as hotelId, r.hotelName as hotelName, r.status as status,
        r.submittedBy as submittedBy, r.submittedAt as submittedAt,
        r.reviewedBy as reviewedBy, r.reviewedAt as reviewedAt,
        r.reason as reason, r.version as version
      from HotelReviewRequestJpaEntity r
      where (:status is null or r.status = :status) and (:hotelId is null or r.hotelId = :hotelId)
      """)
  Page<Summary> findSummaries(
      @Param("status") HotelReviewRequest.Status status,
      @Param("hotelId") UUID hotelId,
      Pageable pageable);

  interface Summary {
    UUID getId();

    UUID getHotelId();

    String getHotelName();

    HotelReviewRequest.Status getStatus();

    UUID getSubmittedBy();

    Instant getSubmittedAt();

    UUID getReviewedBy();

    Instant getReviewedAt();

    String getReason();

    long getVersion();
  }
}

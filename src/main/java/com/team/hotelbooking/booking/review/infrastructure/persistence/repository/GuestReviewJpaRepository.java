package com.team.hotelbooking.booking.review.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.review.infrastructure.persistence.entity.GuestReviewJpaEntity;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GuestReviewJpaRepository
    extends JpaRepository<GuestReviewJpaEntity, UUID>,
        JpaSpecificationExecutor<GuestReviewJpaEntity> {
  Optional<GuestReviewJpaEntity> findByBookingId(UUID id);

  interface RatingTotal {
    int getRating();

    long getCount();
  }

  @Query(
      "select r.rating as rating,count(r) as count from GuestReviewJpaEntity r where r.hotelId=:hotelId group by r.rating")
  List<RatingTotal> ratingTotals(@Param("hotelId") UUID hotelId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update GuestReviewJpaEntity r set r.reply=:reply,r.repliedBy=:actor,r.repliedAt=:at,r.version=r.version+1 where r.id=:id and r.version=:version")
  int updateReply(
      @Param("id") UUID id,
      @Param("version") long version,
      @Param("reply") String reply,
      @Param("actor") UUID actor,
      @Param("at") Instant at);
}

package com.team.hotelbooking.booking.review.infrastructure.persistence.adapter;

import com.team.hotelbooking.booking.review.domain.model.GuestReview;
import com.team.hotelbooking.booking.review.domain.repository.GuestReviewRepository;
import com.team.hotelbooking.booking.review.infrastructure.persistence.mapper.GuestReviewPersistenceMapper;
import com.team.hotelbooking.booking.review.infrastructure.persistence.repository.GuestReviewJpaRepository;
import com.team.hotelbooking.shared.pagination.*;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaGuestReviewRepositoryAdapter implements GuestReviewRepository {
  private final GuestReviewJpaRepository reviews;

  public Optional<GuestReview> findByBookingId(UUID id) {
    return reviews.findByBookingId(id).map(GuestReviewPersistenceMapper::toDomain);
  }

  public Optional<GuestReview> findById(UUID id) {
    return reviews.findById(id).map(GuestReviewPersistenceMapper::toDomain);
  }

  public GuestReview add(GuestReview review) {
    var entity = GuestReviewPersistenceMapper.toEntity(review);
    entity.setVersion(null);
    return GuestReviewPersistenceMapper.toDomain(reviews.saveAndFlush(entity));
  }

  public boolean updateReply(GuestReview r, long version) {
    return reviews.updateReply(r.id(), version, r.reply(), r.repliedBy(), r.repliedAt()) == 1;
  }

  public PageResult<GuestReview> list(
      UUID hotelId, Integer rating, Boolean replied, PageQuery page) {
    return PageResultMapper.from(
        reviews.findAll(
            (root, q, cb) -> {
              var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
              predicates.add(cb.equal(root.get("hotelId"), hotelId));
              if (rating != null) predicates.add(cb.equal(root.get("rating"), rating));
              if (replied != null)
                predicates.add(
                    replied ? cb.isNotNull(root.get("reply")) : cb.isNull(root.get("reply")));
              return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            PageRequest.of(
                page.page(),
                page.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))),
        GuestReviewPersistenceMapper::toDomain);
  }

  public Summary summarize(UUID hotelId) {
    var totals = reviews.ratingTotals(hotelId);
    long count = 0, sum = 0;
    var histogram = new HashMap<Integer, Long>();
    for (var total : totals) {
      count = Math.addExact(count, total.getCount());
      sum = Math.addExact(sum, Math.multiplyExact(total.getRating(), total.getCount()));
      histogram.put(total.getRating(), total.getCount());
    }
    var counts = new ArrayList<RatingCount>();
    for (int rating = 5; rating >= 1; rating--)
      counts.add(new RatingCount(rating, histogram.getOrDefault(rating, 0L)));
    return new Summary(
        count,
        count == 0
            ? null
            : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP),
        counts);
  }
}

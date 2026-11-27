package com.team.hotelbooking.payment.infrastructure.persistence.adapter;

import com.team.hotelbooking.payment.domain.exception.*;
import com.team.hotelbooking.payment.domain.model.*;
import com.team.hotelbooking.payment.domain.repository.RefundRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.RefundActionJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.mapper.RefundPersistenceMapper;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.*;
import com.team.hotelbooking.shared.pagination.*;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaRefundRepositoryAdapter implements RefundRepository {
  private final RefundJpaRepository refunds;
  private final RefundActionJpaRepository actions;

  public Optional<Refund> findById(UUID id) {
    return refunds.findById(id).map(RefundPersistenceMapper::toDomain);
  }

  public Optional<Refund> findByPaymentId(UUID id) {
    return refunds.findByPaymentId(id).map(RefundPersistenceMapper::toDomain);
  }

  public List<Refund> findByBookingId(UUID id) {
    return refunds.findByBookingIdOrderByCreatedAtAscIdAsc(id).stream()
        .map(RefundPersistenceMapper::toDomain)
        .toList();
  }

  public Refund add(Refund r) {
    var e = RefundPersistenceMapper.toEntity(r);
    e.setVersion(null);
    return RefundPersistenceMapper.toDomain(refunds.saveAndFlush(e));
  }

  public boolean update(Refund r, long version, RefundAction a) {
    try {
      if (refunds.update(
              r.id(),
              version,
              r.status(),
              r.settlementReference(),
              r.note(),
              r.updatedAt(),
              r.completedAt())
          != 1) return false;
      actions.saveAndFlush(
          new RefundActionJpaEntity(
              a.id(),
              a.refundId(),
              a.actorId(),
              a.fromStatus(),
              a.toStatus(),
              a.note(),
              a.settlementReference(),
              a.createdAt(),
              a.version()));
      return true;
    } catch (DataIntegrityViolationException error) {
      for (Throwable cause = error; cause != null; cause = cause.getCause())
        if (cause.getMessage() != null
            && cause.getMessage().contains("uq_payment_refunds_reference"))
          throw new FinanceException(FinanceErrorCode.REFUND_REFERENCE_CONFLICT);
      throw error;
    }
  }

  public List<RefundAction> history(UUID id) {
    return actions.findByRefundIdOrderByVersionAsc(id).stream()
        .map(
            a ->
                new RefundAction(
                    a.getId(),
                    a.getRefundId(),
                    a.getActorId(),
                    a.getFromStatus(),
                    a.getToStatus(),
                    a.getNote(),
                    a.getSettlementReference(),
                    a.getCreatedAt(),
                    a.getVersion()))
        .toList();
  }

  public PageResult<Refund> list(
      UUID customerId,
      Set<UUID> hotelIds,
      Refund.Status status,
      UUID bookingId,
      String keyword,
      PageQuery page) {
    String pattern =
        "%"
            + keyword
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            + "%";
    return PageResultMapper.from(
        refunds.findAll(
            (root, query, cb) -> {
              var clauses = new ArrayList<jakarta.persistence.criteria.Predicate>();
              if (customerId != null) clauses.add(cb.equal(root.get("customerId"), customerId));
              if (hotelIds != null)
                clauses.add(
                    hotelIds.isEmpty() ? cb.disjunction() : root.get("hotelId").in(hotelIds));
              if (status != null) clauses.add(cb.equal(root.get("status"), status));
              if (bookingId != null) clauses.add(cb.equal(root.get("bookingId"), bookingId));
              if (!keyword.isEmpty())
                clauses.add(
                    cb.or(
                        cb.like(cb.lower(root.get("bookingCode")), pattern, '!'),
                        cb.like(cb.lower(root.get("bookerName")), pattern, '!')));
              return cb.and(clauses.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            PageRequest.of(
                page.page(),
                page.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))),
        RefundPersistenceMapper::toDomain);
  }
}

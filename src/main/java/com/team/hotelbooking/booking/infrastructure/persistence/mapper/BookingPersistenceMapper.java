package com.team.hotelbooking.booking.infrastructure.persistence.mapper;

import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Booking.BookingItem;
import com.team.hotelbooking.booking.domain.model.Booking.BookingNightPrice;
import com.team.hotelbooking.booking.domain.model.Booking.ContactSnapshot;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingItemJpaEntity;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingJpaEntity;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingNightPriceIdEmbeddable;
import com.team.hotelbooking.booking.infrastructure.persistence.entity.BookingNightPriceJpaEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingPersistenceMapper {

  public BookingJpaEntity toBookingEntity(Booking domain) {
    BookingJpaEntity entity = new BookingJpaEntity();
    entity.setId(domain.id());
    entity.setBookingCode(domain.bookingCode());
    entity.setCustomerId(domain.customerId());
    entity.setHotelId(domain.hotelId());
    entity.setBookerNameSnapshot(domain.booker().fullName());
    entity.setBookerEmailSnapshot(domain.booker().email());
    entity.setBookerPhoneSnapshot(domain.booker().phone());
    entity.setGuestNameSnapshot(domain.guest().fullName());
    entity.setGuestEmailSnapshot(domain.guest().email());
    entity.setGuestPhoneSnapshot(domain.guest().phone());
    entity.setCheckIn(domain.checkIn());
    entity.setCheckOut(domain.checkOut());
    entity.setDays((short) domain.days());
    entity.setNights((short) domain.nights());
    entity.setCheckInTimeSnapshot(domain.standardCheckInTime());
    entity.setCheckOutTimeSnapshot(domain.standardCheckOutTime());
    entity.setExpectedCheckInTime(domain.expectedCheckInTime());
    entity.setExpectedCheckOutTime(domain.expectedCheckOutTime());
    entity.setAdults((short) domain.adults());
    entity.setChildren((short) domain.children());
    entity.setStatus(BookingJpaEntity.DbStatus.valueOf(domain.status().name()));
    entity.setCurrency(domain.currency());
    entity.setRoomSubtotalAmount(domain.roomSubtotalAmount());
    entity.setEarlyCheckInSurchargeAmount(domain.earlyCheckInSurchargeAmount());
    entity.setLateCheckOutSurchargeAmount(domain.lateCheckOutSurchargeAmount());
    entity.setExtraGuestSurchargeAmount(domain.extraGuestSurchargeAmount());
    entity.setSurchargeAmount(domain.surchargeAmount());
    entity.setSubtotalAmount(domain.subtotalAmount());
    entity.setDiscountAmount(domain.discountAmount());
    entity.setTotalAmount(domain.totalAmount());
    entity.setExpiresAt(domain.expiresAt());
    entity.setCreatedAt(domain.createdAt());
    entity.setUpdatedAt(domain.updatedAt());
    entity.setVersion(domain.version());
    return entity;
  }

  public BookingItemJpaEntity toItemEntity(Booking booking, BookingItem item) {
    BookingItemJpaEntity entity = new BookingItemJpaEntity();
    entity.setId(item.id());
    entity.setBookingId(booking.id());
    entity.setHotelId(booking.hotelId());
    entity.setRoomTypeId(item.roomTypeId());
    entity.setRoomTypeNameSnapshot(item.roomTypeNameSnapshot());
    entity.setQuantity((short) item.quantity());
    entity.setAdults((short) item.adults());
    entity.setChildren((short) item.children());
    entity.setExtraGuests((short) item.extraGuests());
    entity.setRoomSubtotalAmount(item.roomSubtotalAmount());
    entity.setEarlyCheckInSurchargeAmount(item.earlyCheckInSurchargeAmount());
    entity.setLateCheckOutSurchargeAmount(item.lateCheckOutSurchargeAmount());
    entity.setExtraGuestSurchargeAmount(item.extraGuestSurchargeAmount());
    entity.setSurchargeAmount(item.surchargeAmount());
    entity.setSubtotalAmount(item.subtotalAmount());
    return entity;
  }

  public List<BookingNightPriceJpaEntity> toNightPriceEntities(BookingItem item) {
    return item.nightlyPrices().stream()
        .map(
            np ->
                new BookingNightPriceJpaEntity(
                    new BookingNightPriceIdEmbeddable(item.id(), np.stayDate()), np.unitPrice()))
        .toList();
  }

  public Booking toDomain(
      BookingJpaEntity entity,
      List<BookingItemJpaEntity> itemEntities,
      Map<UUID, List<BookingNightPriceJpaEntity>> nightlyByItemId) {

    List<BookingItem> items =
        itemEntities == null
            ? List.of()
            : itemEntities.stream()
                .map(
                    ie -> {
                      List<BookingNightPrice> nights =
                          nightlyByItemId.getOrDefault(ie.getId(), List.of()).stream()
                              .map(
                                  ne ->
                                      new BookingNightPrice(
                                          ne.getId().getStayDate(), ne.getUnitPrice()))
                              .toList();
                      return new BookingItem(
                          ie.getId(),
                          ie.getRoomTypeId(),
                          ie.getRoomTypeNameSnapshot(),
                          ie.getQuantity(),
                          ie.getAdults(),
                          ie.getChildren(),
                          ie.getExtraGuests(),
                          ie.getRoomSubtotalAmount(),
                          ie.getEarlyCheckInSurchargeAmount(),
                          ie.getLateCheckOutSurchargeAmount(),
                          ie.getExtraGuestSurchargeAmount(),
                          ie.getSurchargeAmount(),
                          ie.getSubtotalAmount(),
                          nights);
                    })
                .toList();

    return Booking.rehydrate(
        entity.getId(),
        entity.getBookingCode(),
        entity.getCustomerId(),
        entity.getHotelId(),
        new ContactSnapshot(
            entity.getBookerNameSnapshot(),
            entity.getBookerEmailSnapshot(),
            entity.getBookerPhoneSnapshot()),
        new ContactSnapshot(
            entity.getGuestNameSnapshot(),
            entity.getGuestEmailSnapshot(),
            entity.getGuestPhoneSnapshot()),
        entity.getCheckIn(),
        entity.getCheckOut(),
        entity.getDays(),
        entity.getNights(),
        entity.getCheckInTimeSnapshot(),
        entity.getCheckOutTimeSnapshot(),
        entity.getExpectedCheckInTime(),
        entity.getExpectedCheckOutTime(),
        entity.getAdults(),
        entity.getChildren(),
        Booking.Status.valueOf(entity.getStatus().name()),
        entity.getCurrency(),
        entity.getRoomSubtotalAmount(),
        entity.getEarlyCheckInSurchargeAmount(),
        entity.getLateCheckOutSurchargeAmount(),
        entity.getExtraGuestSurchargeAmount(),
        entity.getSurchargeAmount(),
        entity.getSubtotalAmount(),
        entity.getDiscountAmount(),
        entity.getTotalAmount(),
        entity.getExpiresAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion() == null ? 0L : entity.getVersion(),
        items);
  }
}

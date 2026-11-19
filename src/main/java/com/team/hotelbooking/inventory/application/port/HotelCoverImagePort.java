package com.team.hotelbooking.inventory.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

// Ảnh bìa theo lô (chỉ mediaId) lấy từ module media; khách sạn không có ảnh bìa không có trong map.
public interface HotelCoverImagePort {

  Map<UUID, UUID> findCoverImageIds(Collection<UUID> hotelIds);
}

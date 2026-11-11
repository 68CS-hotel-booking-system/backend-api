package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.inventory.application.port.HotelCoverImagePort;
import com.team.hotelbooking.media.HotelImageDirectory;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelCoverImageAdapter implements HotelCoverImagePort {

  private final HotelImageDirectory hotelImageDirectory;

  @Override
  public Map<UUID, UUID> findCoverImageIds(Collection<UUID> hotelIds) {
    return hotelImageDirectory.findCoverImages(hotelIds);
  }
}

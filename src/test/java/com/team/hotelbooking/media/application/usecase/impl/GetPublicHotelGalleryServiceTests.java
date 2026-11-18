package com.team.hotelbooking.media.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.HotelImage;
import com.team.hotelbooking.media.domain.model.HotelImageAlbum;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPublicHotelGalleryServiceTests {
  @Mock HotelImageAccess hotels;
  @Mock HotelImageRepository images;
  @Mock HotelImageAlbumRepository albums;

  @Test
  void shouldRejectUnpublishedHotelBeforeReadingImages() {
    var id = UUID.randomUUID();
    var service = new GetPublicHotelGalleryService(hotels, images, albums);
    assertThrows(MediaException.class, () -> service.execute(id));
    then(images).shouldHaveNoInteractions();
    then(albums).shouldHaveNoInteractions();
  }

  @Test
  void shouldReturnPublishedPhotosAndOnlyNonEmptyAlbums() {
    var id = UUID.randomUUID();
    var album = UUID.randomUUID();
    var photo = UUID.randomUUID();
    var now = Instant.parse("2026-11-15T00:00:00Z");
    given(hotels.isPublished(id)).willReturn(true);
    given(images.findPublicByHotelId(id))
        .willReturn(List.of(new HotelImage(photo, album, 0, true)));
    given(albums.findByHotelId(id))
        .willReturn(
            List.of(
                new HotelImageAlbum(album, id, "Sảnh", null, 0, now, now),
                new HotelImageAlbum(UUID.randomUUID(), id, "Trống", null, 1, now, now)));
    var result = new GetPublicHotelGalleryService(hotels, images, albums).execute(id);
    assertEquals(photo, result.images().getFirst().mediaId());
    assertEquals(1, result.albums().size());
    assertEquals(album, result.albums().getFirst().id());
  }
}

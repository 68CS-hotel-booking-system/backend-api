package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.CreateHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.command.DeleteHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.command.ListHotelImageAlbumsQuery;
import com.team.hotelbooking.media.application.command.ReorderHotelImageAlbumsCommand;
import com.team.hotelbooking.media.application.command.UpdateHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.result.DeleteHotelImageAlbumResult;
import com.team.hotelbooking.media.application.result.HotelImageAlbumResult;
import com.team.hotelbooking.media.application.usecase.CreateHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.DeleteHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.ListHotelImageAlbumsUseCase;
import com.team.hotelbooking.media.application.usecase.ReorderHotelImageAlbumsUseCase;
import com.team.hotelbooking.media.application.usecase.UpdateHotelImageAlbumUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.HotelImageAlbum;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HotelImageAlbumService
    implements ListHotelImageAlbumsUseCase,
        CreateHotelImageAlbumUseCase,
        UpdateHotelImageAlbumUseCase,
        ReorderHotelImageAlbumsUseCase,
        DeleteHotelImageAlbumUseCase {

  private final HotelImageAlbumRepository albums;
  private final HotelImageAccess hotels;
  private final Clock clock;

  public HotelImageAlbumService(
      HotelImageAlbumRepository albums, HotelImageAccess hotels, Clock clock) {

    this.albums = albums;
    this.hotels = hotels;
    this.clock = clock;
  }

  @Override
  public List<HotelImageAlbumResult> execute(ListHotelImageAlbumsQuery query) {

    hotels.requireManage(query.actorId(), query.hotelId());

    return albums.findByHotelId(query.hotelId()).stream().map(HotelImageAlbumResult::from).toList();
  }

  @Override
  public HotelImageAlbumResult execute(CreateHotelImageAlbumCommand command) {

    hotels.lockForUpdate(command.actorId(), command.hotelId(), command.expectedVersion());

    List<HotelImageAlbum> current = albums.findByHotelId(command.hotelId());

    int maxOrder = current.stream().mapToInt(HotelImageAlbum::sortOrder).max().orElse(-1);

    if (maxOrder == Integer.MAX_VALUE) {
      throw invalid("Không thể thêm album vào cuối danh sách.");
    }

    Instant now = clock.instant();

    HotelImageAlbum album =
        new HotelImageAlbum(
            UUID.randomUUID(),
            command.hotelId(),
            command.name(),
            command.description(),
            maxOrder + 1,
            now,
            now);

    albums.add(album);

    return HotelImageAlbumResult.from(album);
  }

  @Override
  public HotelImageAlbumResult execute(UpdateHotelImageAlbumCommand command) {

    if (command.name() == null && command.description() == null) {
      throw invalid("Cần cung cấp tên hoặc mô tả để cập nhật.");
    }

    hotels.lockForUpdate(command.actorId(), command.hotelId(), command.expectedVersion());

    HotelImageAlbum current = requireAlbum(command.hotelId(), command.albumId());

    HotelImageAlbum updated =
        current.update(command.name(), command.description(), clock.instant());

    albums.update(updated);

    return HotelImageAlbumResult.from(updated);
  }

  @Override
  public List<HotelImageAlbumResult> execute(ReorderHotelImageAlbumsCommand command) {

    if (new HashSet<>(command.albumIds()).size() != command.albumIds().size()) {
      throw invalid("Danh sách album không được trùng ID.");
    }

    hotels.lockForUpdate(command.actorId(), command.hotelId(), command.expectedVersion());

    List<HotelImageAlbum> current = albums.findByHotelId(command.hotelId());

    Map<UUID, HotelImageAlbum> byId = new HashMap<>();

    for (HotelImageAlbum album : current) {
      byId.put(album.id(), album);
    }

    if (!byId.keySet().equals(new HashSet<>(command.albumIds()))) {
      throw invalid(
          "Phải gửi đầy đủ các album của khách sạn, " + "không thiếu hoặc thêm album khác.");
    }

    Instant now = clock.instant();
    List<HotelImageAlbumResult> result = new ArrayList<>();

    for (int index = 0; index < command.albumIds().size(); index++) {
      UUID albumId = command.albumIds().get(index);

      HotelImageAlbum reordered = byId.get(albumId).reorder(index, now);

      albums.update(reordered);
      result.add(HotelImageAlbumResult.from(reordered));
    }

    return List.copyOf(result);
  }

  @Override
  public DeleteHotelImageAlbumResult execute(DeleteHotelImageAlbumCommand command) {

    hotels.lockForUpdate(command.actorId(), command.hotelId(), command.expectedVersion());

    requireAlbum(command.hotelId(), command.albumId());

    albums.deleteAndUngroupImages(command.hotelId(), command.albumId());

    return new DeleteHotelImageAlbumResult(true);
  }

  private HotelImageAlbum requireAlbum(UUID hotelId, UUID albumId) {

    return albums
        .findByIdAndHotelId(albumId, hotelId)
        .orElseThrow(
            () ->
                new MediaException(
                    MediaErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy album trong khách sạn."));
  }

  private static MediaException invalid(String message) {
    return new MediaException(MediaErrorCode.VALIDATION_FAILED, message);
  }
}

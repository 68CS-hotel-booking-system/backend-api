// DeleteHotelImageAlbumResponse.java
package com.team.hotelbooking.media.api.dto.response;

import com.team.hotelbooking.media.application.result.DeleteHotelImageAlbumResult;

public record DeleteHotelImageAlbumResponse(boolean accepted) {

  public static DeleteHotelImageAlbumResponse from(DeleteHotelImageAlbumResult result) {

    return new DeleteHotelImageAlbumResponse(result.accepted());
  }
}

package com.team.hotelbooking.media.application.port;

public interface ImageInspector {

  ImageInfo inspect(byte[] content);

  record ImageInfo(String contentType, String extension) {}
}

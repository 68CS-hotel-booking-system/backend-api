package com.team.hotelbooking.media.application.port;

import java.net.URI;
import java.time.Duration;

public interface ObjectStorage {

  void upload(String objectKey, byte[] content, String contentType);

  void delete(String objectKey);

  URI createDownloadUrl(String objectKey, Duration validity);
}

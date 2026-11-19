package com.team.hotelbooking.media.infrastructure.integration;

import com.team.hotelbooking.media.application.port.ImageInspector;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageIoInspector implements ImageInspector {

  private final long maxPixels;

  public ImageIoInspector(@Value("${media.upload.max-pixels:16000000}") long maxPixels) {

    if (maxPixels <= 0) {
      throw new IllegalArgumentException("media.upload.max-pixels phải lớn hơn 0.");
    }

    this.maxPixels = maxPixels;
  }

  @Override
  public ImageInfo inspect(byte[] content) {
    try (MemoryCacheImageInputStream input =
        new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {

      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

      if (!readers.hasNext()) {
        throw unsupported();
      }

      ImageReader reader = readers.next();

      try {
        reader.setInput(input, true, true);

        String format = reader.getFormatName().toUpperCase(Locale.ROOT);

        ImageInfo info =
            switch (format) {
              case "JPEG", "JPG" -> new ImageInfo("image/jpeg", "jpg");
              case "PNG" -> new ImageInfo("image/png", "png");
              case "WEBP" -> new ImageInfo("image/webp", "webp");
              default -> throw unsupported();
            };

        int width = reader.getWidth(0);
        int height = reader.getHeight(0);

        if (width <= 0 || height <= 0 || (long) width * height > maxPixels) {
          throw new MediaException(
              MediaErrorCode.VALIDATION_FAILED, "Kích thước ảnh không hợp lệ hoặc quá lớn.");
        }

        BufferedImage decoded = reader.read(0);

        if (decoded == null) {
          throw unsupported();
        }

        decoded.flush();

        return info;
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw unsupported();
    }
  }

  private static MediaException unsupported() {
    return new MediaException(MediaErrorCode.UNSUPPORTED_MEDIA_TYPE);
  }
}

package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Vị trí package-info.java trong mã nguồn chính")
class PackageInfoPlacementTests {

  private static final Path ROOT = Path.of("src", "main", "java", "com", "team", "hotelbooking");

  @Nested
  @DisplayName("Duyệt src/main/java/com/team/hotelbooking")
  class ScanTests {

    @Test
    @DisplayName("Chỉ module shared được có package-info.java")
    void shouldOnlyContainSharedPackageInfoWhenScanningMainSources() throws IOException {
      // Given
      assertTrue(Files.isDirectory(ROOT), "Không tìm thấy thư mục mã nguồn: " + ROOT);

      // When
      List<String> packageInfos;
      try (Stream<Path> files = Files.walk(ROOT)) {
        packageInfos =
            files
                .filter(path -> path.getFileName().toString().equals("package-info.java"))
                .map(path -> ROOT.relativize(path).toString().replace('\\', '/'))
                .sorted()
                .toList();
      }

      // Then
      assertEquals(
          List.of("shared/package-info.java"),
          packageInfos,
          "package-info.java chỉ được đặt ở shared; module khác giao tiếp qua Directory");
    }
  }
}

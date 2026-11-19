package com.team.hotelbooking.media.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "media_objects")
@Getter
@Setter
@NoArgsConstructor
public class MediaJpaEntity {

  public enum DbStatus {
    PENDING,
    READY,
    DELETED
  }

  @Id private UUID id;

  @Column(name = "storage_provider", nullable = false, length = 32)
  private String storageProvider;

  @Column(name = "bucket_name", nullable = false, length = 100)
  private String bucketName;

  @Column(name = "object_key", nullable = false, length = 512)
  private String objectKey;

  @Column(name = "original_filename", nullable = false, length = 255)
  private String originalFilename;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "checksum_sha256", length = 64)
  private String checksumSha256;

  @Column(nullable = false, length = 32)
  private String purpose;

  @Column(name = "hotel_id")
  private UUID hotelId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "media_status__t")
  private DbStatus status;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}

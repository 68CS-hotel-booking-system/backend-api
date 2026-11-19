package com.team.hotelbooking.media.infrastructure.integration;

import com.team.hotelbooking.media.application.port.ObjectStorage;
import com.team.hotelbooking.media.application.port.ObjectStorageException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

public final class R2ObjectStorageAdapter implements ObjectStorage {

  private final S3Client client;
  private final S3Presigner presigner;
  private final String bucket;

  public R2ObjectStorageAdapter(S3Client client, S3Presigner presigner, String bucket) {

    this.client = Objects.requireNonNull(client);
    this.presigner = Objects.requireNonNull(presigner);

    if (bucket == null || bucket.isBlank()) {
      throw new IllegalArgumentException("storage.r2.bucket không được để trống.");
    }

    this.bucket = bucket;
  }

  @Override
  public void upload(String objectKey, byte[] content, String contentType) {

    PutObjectRequest request =
        PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType(contentType).build();

    try {
      client.putObject(request, RequestBody.fromBytes(content));
    } catch (SdkException exception) {
      throw new ObjectStorageException("Không thể tải ảnh lên kho lưu trữ.", exception);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    } catch (SdkException exception) {
      throw new ObjectStorageException("Không thể xóa ảnh khỏi kho lưu trữ.", exception);
    }
  }

  @Override
  public URI createDownloadUrl(String objectKey, Duration validity) {

    GetObjectRequest objectRequest =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .responseContentDisposition("inline")
            .build();

    GetObjectPresignRequest request =
        GetObjectPresignRequest.builder()
            .signatureDuration(validity)
            .getObjectRequest(objectRequest)
            .build();

    try {
      return URI.create(presigner.presignGetObject(request).url().toString());
    } catch (SdkException exception) {
      throw new ObjectStorageException("Không thể tạo URL truy cập ảnh.", exception);
    }
  }
}

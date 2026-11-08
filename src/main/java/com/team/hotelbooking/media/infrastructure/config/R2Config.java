package com.team.hotelbooking.media.infrastructure.config;

import com.team.hotelbooking.media.application.port.ObjectStorage;
import com.team.hotelbooking.media.infrastructure.integration.R2ObjectStorageAdapter;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class R2Config {

  @Bean(destroyMethod = "close")
  public S3Client r2S3Client(
      @Value("${storage.r2.endpoint}") String endpoint,
      @Value("${storage.r2.access-key-id}") String accessKeyId,
      @Value("${storage.r2.secret-access-key}") String secretAccessKey) {

    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

    S3Configuration configuration =
        S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .chunkedEncodingEnabled(false)
            .build();

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of("auto"))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .serviceConfiguration(configuration)
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .overrideConfiguration(
            builder ->
                builder
                    .apiCallAttemptTimeout(Duration.ofSeconds(10))
                    .apiCallTimeout(Duration.ofSeconds(30)))
        .build();
  }

  @Bean
  public ObjectStorage objectStorage(
      S3Client r2S3Client, S3Presigner r2Presigner, @Value("${storage.r2.bucket}") String bucket) {

    return new R2ObjectStorageAdapter(r2S3Client, r2Presigner, bucket);
  }

  @Bean(destroyMethod = "close")
  public S3Presigner r2Presigner(
      @Value("${storage.r2.endpoint}") String endpoint,
      @Value("${storage.r2.access-key-id}") String accessKeyId,
      @Value("${storage.r2.secret-access-key}") String secretAccessKey) {

    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of("auto"))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}

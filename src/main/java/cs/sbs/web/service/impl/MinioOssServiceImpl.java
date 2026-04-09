package cs.sbs.web.service.impl;

import cs.sbs.web.dto.MinioProperties;
import cs.sbs.web.dto.OssObjectInfo;
import cs.sbs.web.service.OssService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioOssServiceImpl implements OssService {

    private final MinioClient minioClient;
    private final MinioClient presignClient;
    private final MinioProperties properties;

    public MinioOssServiceImpl(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.presignClient = MinioClient.builder()
                .endpoint(properties.getPublicEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public OssObjectInfo upload(String bucket, String objectKey, MultipartFile file) {
        try {
            String normalizedBucket = bucket == null || bucket.isBlank() ? properties.getBucket() : bucket;
            String normalizedObjectKey = normalizeObjectKey(objectKey, file);

            ensureBucketExists(normalizedBucket);

            try (InputStream inputStream = file.getInputStream()) {
                PutObjectArgs args = PutObjectArgs.builder()
                        .bucket(normalizedBucket)
                        .object(normalizedObjectKey)
                        .stream(inputStream, file.getSize(), -1L)
                        .contentType(file.getContentType())
                        .build();
                minioClient.putObject(args);
            }

            String url = presignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(normalizedBucket)
                            .object(normalizedObjectKey)
                            .expiry(properties.getPresignExpirySeconds())
                            .build()
            );
            return new OssObjectInfo(normalizedBucket, normalizedObjectKey, url);
        } catch (Exception ex) {
            throw new RuntimeException("上传文件到 MinIO 失败(endpoint=" + properties.getEndpoint() + "): " + ex.getMessage(), ex);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String normalizeObjectKey(String objectKey, MultipartFile file) {
        String base = objectKey == null ? "" : objectKey.trim();
        if (base.isBlank()) {
            String original = file.getOriginalFilename();
            String safeName = original == null || original.isBlank() ? "file" : original.replaceAll("\\s+", "_");
            return "uploads/" + UUID.randomUUID() + "-" + safeName;
        }
        while (base.startsWith("/")) {
            base = base.substring(1);
        }
        return base;
    }
}

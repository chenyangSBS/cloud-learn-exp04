package cs.sbs.web.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cs.sbs.web.dto.MinioProperties;

@Configuration
public class MinioConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.minio", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}

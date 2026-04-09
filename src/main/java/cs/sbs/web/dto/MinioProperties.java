package cs.sbs.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.minio")
@Validated
@Getter
@Setter
public class MinioProperties {

    private boolean enabled;

    @NotBlank
    private String endpoint;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String bucket;

    @NotBlank
    private String publicEndpoint;

    @Min(1)
    private int presignExpirySeconds;

}

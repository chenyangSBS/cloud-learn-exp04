package cs.sbs.web.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class OssObjectInfo {

    private final String bucket;
    private final String objectKey;
    private final String url;
    private final Instant expiresAt;

}

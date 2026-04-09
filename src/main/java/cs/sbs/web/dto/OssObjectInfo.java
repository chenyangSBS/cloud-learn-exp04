package cs.sbs.web.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OssObjectInfo {

    private final String bucket;
    private final String objectKey;
    private final String url;

}

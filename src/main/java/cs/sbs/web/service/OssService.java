package cs.sbs.web.service;

import cs.sbs.web.dto.OssObjectInfo;
import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    OssObjectInfo upload(String bucket, String objectKey, MultipartFile file);

    OssObjectInfo presignGet(String bucket, String objectKey);
}

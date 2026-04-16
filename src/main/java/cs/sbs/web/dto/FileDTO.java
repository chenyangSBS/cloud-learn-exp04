package cs.sbs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class FileDTO {
    private String fileName;
    private String fileUrl;
}

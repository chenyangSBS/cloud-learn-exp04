package cs.sbs.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class CourseEvent {

    private Long courseId;
    private String type;
    private Object data;
    private LocalDateTime timestamp;

    public CourseEvent(Long courseId, String type, Object data) {
        this.courseId = courseId;
        this.type = type;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

}

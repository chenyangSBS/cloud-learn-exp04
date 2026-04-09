package cs.sbs.web.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface CourseSseService {

    SseEmitter subscribe(Long courseId);

    void publish(Long courseId, String type, Object data);
}

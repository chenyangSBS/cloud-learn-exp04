package cs.sbs.web.service.impl;

import cs.sbs.web.dto.CourseEvent;
import cs.sbs.web.service.CourseSseService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CourseSseServiceImpl implements CourseSseService {

    private final Map<Long, List<SseEmitter>> emittersByCourseId = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long courseId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByCourseId.computeIfAbsent(courseId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(courseId, emitter));
        emitter.onTimeout(() -> remove(courseId, emitter));
        emitter.onError(ex -> remove(courseId, emitter));

        sendToEmitter(emitter, "connected", new CourseEvent(courseId, "connected", "ok"));
        return emitter;
    }

    @Override
    public void publish(Long courseId, String type, Object data) {
        List<SseEmitter> emitters = emittersByCourseId.get(courseId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        CourseEvent event = new CourseEvent(courseId, type, data);
        for (SseEmitter emitter : emitters) {
            sendToEmitter(emitter, type, event);
        }
    }

    private void sendToEmitter(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void remove(Long courseId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByCourseId.get(courseId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByCourseId.remove(courseId);
        }
    }
}

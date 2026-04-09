package cs.sbs.web.service;

import cs.sbs.web.entity.Course;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface CourseService {

    List<Course> findAll();

    Course findById(Long id);

    List<Course> search(String keyword);

    Course create(Course course);

    Course update(Long id, Course course);

    void delete(Long id);

    void incrementStudentCount(Long id);

    Course updateCoverUrl(Long id, String coverUrl);

}

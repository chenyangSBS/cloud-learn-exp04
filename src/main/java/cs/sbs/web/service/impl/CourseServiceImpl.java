package cs.sbs.web.service.impl;

import cs.sbs.web.dto.CourseEvent;
import cs.sbs.web.entity.Course;
import cs.sbs.web.exception.ResourceNotFoundException;
import cs.sbs.web.repository.CourseRepository;
import cs.sbs.web.service.CourseService;
import cs.sbs.web.service.CourseSseService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseSseService courseSseService;

    public CourseServiceImpl(CourseRepository courseRepository, CourseSseService courseSseService) {
        this.courseRepository = courseRepository;
        this.courseSseService = courseSseService;
    }

    @Override
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Override
    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("课程不存在: id=" + id));
    }

    @Override
    public List<Course> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return courseRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public Course create(Course course) {
        normalizeCourse(course);
        course.setId(null);
        Course created = courseRepository.save(course);
        courseSseService.publish(created.getId(), "courseCreated", created);
        return created;
    }

    @Override
    public Course update(Long id, Course course) {
        Course existing = findById(id);
        normalizeCourse(course);

        existing.setTitle(course.getTitle());
        existing.setInstructor(course.getInstructor());
        existing.setPrice(course.getPrice());
        existing.setDescription(course.getDescription());
        existing.setDuration(course.getDuration());
        existing.setStudentCount(course.getStudentCount());

        Course updated = courseRepository.save(existing);
        courseSseService.publish(updated.getId(), "courseUpdated", updated);
        return updated;
    }

    @Override
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("课程不存在: id=" + id);
        }
        courseRepository.deleteById(id);
        courseSseService.publish(id, "courseDeleted", id);
    }

    @Override
    public void incrementStudentCount(Long id) {
        Course course = findById(id);
        Integer count = course.getStudentCount() == null ? 0 : course.getStudentCount();
        course.setStudentCount(count + 1);
        Course updated = courseRepository.save(course);
        courseSseService.publish(updated.getId(), "studentCountUpdated", updated.getStudentCount());
    }

    @Override
    public Course updateCoverUrl(Long id, String coverUrl) {
        Course course = findById(id);
        course.setCoverUrl(coverUrl);
        Course updated = courseRepository.save(course);
        courseSseService.publish(updated.getId(), "coverUploaded", updated.getCoverUrl());
        return updated;
    }

    private void normalizeCourse(Course course) {
        if (course.getStudentCount() == null) {
            course.setStudentCount(0);
        }
    }
}

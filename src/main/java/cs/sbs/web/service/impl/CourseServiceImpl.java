package cs.sbs.web.service.impl;

import cs.sbs.web.dto.OssObjectInfo;
import cs.sbs.web.entity.Course;
import cs.sbs.web.exception.ResourceNotFoundException;
import cs.sbs.web.repository.CourseRepository;
import cs.sbs.web.service.CourseService;
import cs.sbs.web.service.CourseSseService;
import cs.sbs.web.service.OssService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private static final long COVER_URL_REFRESH_BUFFER_SECONDS = 60;

    private final CourseRepository courseRepository;
    private final CourseSseService courseSseService;
    private final OssService ossService;

    public CourseServiceImpl(CourseRepository courseRepository, CourseSseService courseSseService, OssService ossService) {
        this.courseRepository = courseRepository;
        this.courseSseService = courseSseService;
        this.ossService = ossService;
    }

    @Override
    public List<Course> findAll() {
        List<Course> courses = courseRepository.findAll();
        return courses.stream().map(this::refreshCoverUrlIfNeeded).toList();
    }

    @Override
    public Course findById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("课程不存在: id=" + id));
        return refreshCoverUrlIfNeeded(course);
    }

    @Override
    public List<Course> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return courseRepository.findByTitleContainingIgnoreCase(keyword).stream()
                .map(this::refreshCoverUrlIfNeeded)
                .toList();
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
    public Course updateCoverObject(Long id, String bucket, String objectKey, String presignedUrl, Instant expiresAt) {
        Course course = findById(id);
        course.setCoverBucket(bucket);
        course.setCoverObjectKey(objectKey);
        course.setCoverUrl(presignedUrl);
        course.setCoverUrlExpiresAt(expiresAt);
        Course updated = courseRepository.save(course);
        courseSseService.publish(updated.getId(), "coverUploaded", updated.getCoverUrl());
        return updated;
    }

    private Course refreshCoverUrlIfNeeded(Course course) {
        if (course == null) {
            return null;
        }
        String objectKey = course.getCoverObjectKey();
        if (objectKey == null || objectKey.isBlank()) {
            return course;
        }

        Instant now = Instant.now();
        Instant expiresAt = course.getCoverUrlExpiresAt();
        boolean shouldRefresh = expiresAt == null
                || !now.isBefore(expiresAt.minusSeconds(COVER_URL_REFRESH_BUFFER_SECONDS));
        if (!shouldRefresh) {
            return course;
        }

        OssObjectInfo info = ossService.presignGet(course.getCoverBucket(), objectKey);
        course.setCoverBucket(info.getBucket());
        course.setCoverObjectKey(info.getObjectKey());
        course.setCoverUrl(info.getUrl());
        course.setCoverUrlExpiresAt(info.getExpiresAt());
        return courseRepository.save(course);
    }

    private void normalizeCourse(Course course) {
        if (course.getStudentCount() == null) {
            course.setStudentCount(0);
        }
    }
}

package cs.sbs.web.service;

import cs.sbs.web.entity.Course;
import cs.sbs.web.exception.ResourceNotFoundException;
import cs.sbs.web.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
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
        return courseRepository.save(course);
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

        return courseRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("课程不存在: id=" + id);
        }
        courseRepository.deleteById(id);
    }

    @Override
    public void incrementStudentCount(Long id) {
        Course course = findById(id);
        Integer count = course.getStudentCount() == null ? 0 : course.getStudentCount();
        course.setStudentCount(count + 1);
        courseRepository.save(course);
    }

    private void normalizeCourse(Course course) {
        if (course.getStudentCount() == null) {
            course.setStudentCount(0);
        }
    }
}

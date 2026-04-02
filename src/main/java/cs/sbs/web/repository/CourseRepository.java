package cs.sbs.web.repository;

import cs.sbs.web.entity.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {

    List<Course> findAll();

    Optional<Course> findById(Long id);

    boolean existsById(Long id);

    Course save(Course course);

    void deleteById(Long id);

    List<Course> findByTitleContainingIgnoreCase(String keyword);
}

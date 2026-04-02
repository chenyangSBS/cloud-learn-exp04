package cs.sbs.web.repository;

import cs.sbs.web.entity.Course;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCourseRepository implements CourseRepository {

    private final Map<Long, Course> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    @Override
    public List<Course> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Course::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<Course> findById(Long id) {
        Course course = store.get(id);
        if (course == null) {
            return Optional.empty();
        }
        return Optional.of(copy(course));
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public Course save(Course course) {
        LocalDateTime now = LocalDateTime.now();

        if (course.getId() == null) {
            course.setId(idSequence.incrementAndGet());
            course.setCreatedAt(now);
        } else {
            idSequence.updateAndGet(current -> Math.max(current, course.getId()));
            Course existing = store.get(course.getId());
            if (existing != null) {
                course.setCreatedAt(existing.getCreatedAt());
            } else if (course.getCreatedAt() == null) {
                course.setCreatedAt(now);
            }
        }

        course.setUpdatedAt(now);

        Course stored = copy(course);
        store.put(stored.getId(), stored);
        return copy(stored);
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public List<Course> findByTitleContainingIgnoreCase(String keyword) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return findAll();
        }
        List<Course> result = new ArrayList<>();
        for (Course course : store.values()) {
            if (course.getTitle() != null && course.getTitle().toLowerCase(Locale.ROOT).contains(normalized)) {
                result.add(copy(course));
            }
        }
        result.sort(Comparator.comparing(Course::getId));
        return result;
    }

    public void clear() {
        store.clear();
        idSequence.set(0);
    }

    private Course copy(Course source) {
        if (source == null) {
            return null;
        }
        return new Course(
                source.getId(),
                source.getTitle(),
                source.getInstructor(),
                source.getPrice(),
                source.getDescription(),
                source.getDuration(),
                source.getStudentCount(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}

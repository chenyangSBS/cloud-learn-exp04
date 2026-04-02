package cs.sbs.web.controller;

import cs.sbs.web.dto.ApiResponse;
import cs.sbs.web.dto.CourseDTO;
import cs.sbs.web.entity.Course;
import cs.sbs.web.service.CourseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        log.info("GET /api/courses");
        List<Course> courses = courseService.findAll();
        return ResponseEntity.ok(ApiResponse.success("查询成功", courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable Long id) {
        log.info("GET /api/courses/{}", id);
        Course course = courseService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", course));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Course>>> searchCourses(@RequestParam(required = false) String keyword) {
        log.info("GET /api/courses/search?keyword={}", keyword);
        List<Course> courses = courseService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success("搜索成功", courses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody CourseDTO courseDTO) {
        log.info("POST /api/courses");
        Course created = courseService.create(convertToEntity(courseDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO courseDTO
    ) {
        log.info("PUT /api/courses/{}", id);
        Course updated = courseService.update(id, convertToEntity(courseDTO));
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        log.info("DELETE /api/courses/{}", id);
        courseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PatchMapping("/{id}/students")
    public ResponseEntity<ApiResponse<Void>> incrementStudentCount(@PathVariable Long id) {
        log.info("PATCH /api/courses/{}/students", id);
        courseService.incrementStudentCount(id);
        return ResponseEntity.ok(ApiResponse.success("学习人数更新成功", null));
    }

    private Course convertToEntity(CourseDTO dto) {
        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setInstructor(dto.getInstructor());
        course.setPrice(dto.getPrice());
        course.setDescription(dto.getDescription());
        course.setDuration(dto.getDuration());
        course.setStudentCount(dto.getStudentCount());
        return course;
    }
}

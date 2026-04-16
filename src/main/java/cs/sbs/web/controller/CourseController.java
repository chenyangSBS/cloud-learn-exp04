package cs.sbs.web.controller;

import cs.sbs.web.dto.ApiResponse;
import cs.sbs.web.dto.CourseDTO;
import cs.sbs.web.dto.FileDTO;
import cs.sbs.web.dto.OssObjectInfo;
import cs.sbs.web.entity.Course;
import cs.sbs.web.exception.BadRequestException;
import cs.sbs.web.service.CourseService;
import cs.sbs.web.service.CourseSseService;
import cs.sbs.web.service.OssService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    @Value("${server.port}")
    public String port;

    private final CourseService courseService;
    private final CourseSseService courseSseService;
    private final OssService ossService;

    public CourseController(CourseService courseService, CourseSseService courseSseService, OssService ossService) {
        this.courseService = courseService;
        this.courseSseService = courseSseService;
        this.ossService = ossService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        log.info("GET /api/courses");
        List<Course> courses = courseService.findAll();
        return ResponseEntity.ok(ApiResponse.success("查询成功", courses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable Long id) {
        log.info("GET /api/courses/{}", id);
        Course course = courseService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", course));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<List<Course>>> searchCourses(@RequestParam(required = false) String keyword) {
        log.info("GET /api/courses/search?keyword={}", keyword);
        List<Course> courses = courseService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success("搜索成功", courses));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody CourseDTO courseDTO) {
        log.info("POST /api/courses");
        Course created = courseService.create(convertToEntity(courseDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO courseDTO
    ) {
        log.info("PUT /api/courses/{}", id);
        Course updated = courseService.update(id, convertToEntity(courseDTO));
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        log.info("DELETE /api/courses/{}", id);
        courseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PatchMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<Void>> incrementStudentCount(@PathVariable Long id) {
        log.info("PATCH /api/courses/{}/students", id);
        courseService.incrementStudentCount(id);
        return ResponseEntity.ok(ApiResponse.success("学习人数更新成功", null));
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public SseEmitter subscribeCourseEvents(@PathVariable Long id) {
        log.info("GET /api/courses/{}/events", id);
        courseService.findById(id);
        return courseSseService.subscribe(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FileDTO>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String upload_url_prefix = "src/main/resources/upload/";
            Path path = Paths.get(upload_url_prefix + file.getOriginalFilename());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(ApiResponse.success(new FileDTO(file.getOriginalFilename(),new StringBuilder().append("http://localhost:").append(port).append("/").append(file.getOriginalFilename()).toString())));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("上传文件失败", 500));
        }
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Course>> uploadCourseCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("POST /api/courses/{}/cover", id);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("上传文件不能为空");
        }

        courseService.findById(id);

        String original = file.getOriginalFilename();
        String safeName = original == null || original.isBlank() ? "cover" : original.replaceAll("\\s+", "_");
        String objectKey = "courses/" + id + "/cover/" + UUID.randomUUID() + "-" + safeName;

        OssObjectInfo info = ossService.upload(null, objectKey, file);
        Course updated = courseService.updateCoverObject(id, info.getBucket(), info.getObjectKey(), info.getUrl(), info.getExpiresAt());
        return ResponseEntity.ok(ApiResponse.success("封面上传成功", updated));
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

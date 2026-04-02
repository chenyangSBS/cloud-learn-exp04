package cs.sbs.web.config;

import cs.sbs.web.entity.Course;
import cs.sbs.web.repository.CourseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseDataInitializer implements ApplicationRunner {

    private final CourseRepository courseRepository;

    public CourseDataInitializer(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!courseRepository.findAll().isEmpty()) {
            return;
        }

        List<Course> courses = List.of(
                create(1L, "Java核心技术（完整版）", "张老师", 199.00, "从Java基础到高级特性，系统学习Java编程", 48, 1200),
                create(2L, "Spring Boot实战开发", "李老师", 299.00, "快速构建企业级Spring应用的最佳实践", 36, 800),
                create(3L, "前端开发从入门到精通", "王老师", 249.00, "HTML/CSS/JavaScript/Vue/React全栈前端", 60, 1500),
                create(4L, "MySQL数据库设计与优化", "赵老师", 159.00, "数据库设计原理与性能优化技巧", 24, 600),
                create(5L, "Redis缓存技术实战", "陈老师", 189.00, "高性能缓存解决方案与实战案例", 20, 450),
                create(6L, "Docker容器化部署", "刘老师", 149.00, "容器技术入门与应用部署实战", 18, 350),
                create(7L, "微服务架构设计", "周老师", 349.00, "Spring Cloud微服务架构设计与实现", 42, 500),
                create(8L, "Python数据分析", "吴老师", 229.00, "Python数据处理与可视化入门", 32, 900)
        );

        for (Course course : courses) {
            courseRepository.save(course);
        }
    }

    private Course create(
            Long id,
            String title,
            String instructor,
            Double price,
            String description,
            Integer duration,
            Integer studentCount
    ) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
        course.setInstructor(instructor);
        course.setPrice(price);
        course.setDescription(description);
        course.setDuration(duration);
        course.setStudentCount(studentCount);
        return course;
    }
}

package cs.sbs.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println();
        System.out.println("🚀 智学云API服务启动成功！");
        System.out.println("📚 课程API: http://localhost:8080/api/courses");
        System.out.println();
    }
}

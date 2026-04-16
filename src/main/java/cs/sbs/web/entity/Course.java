package cs.sbs.web.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public class Course {

    private Long id;
    private String title;
    private String instructor;
    private Double price;
    private String description;
    private Integer duration;
    private Integer studentCount;
    private String coverBucket;
    private String coverObjectKey;
    private String coverUrl;
    private Instant coverUrlExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Course() {
    }

    public Course(
            Long id,
            String title,
            String instructor,
            Double price,
            String description,
            Integer duration,
            Integer studentCount,
            String coverBucket,
            String coverObjectKey,
            String coverUrl,
            Instant coverUrlExpiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.instructor = instructor;
        this.price = price;
        this.description = description;
        this.duration = duration;
        this.studentCount = studentCount;
        this.coverBucket = coverBucket;
        this.coverObjectKey = coverObjectKey;
        this.coverUrl = coverUrl;
        this.coverUrlExpiresAt = coverUrlExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public String getCoverBucket() {
        return coverBucket;
    }

    public void setCoverBucket(String coverBucket) {
        this.coverBucket = coverBucket;
    }

    public String getCoverObjectKey() {
        return coverObjectKey;
    }

    public void setCoverObjectKey(String coverObjectKey) {
        this.coverObjectKey = coverObjectKey;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Instant getCoverUrlExpiresAt() {
        return coverUrlExpiresAt;
    }

    public void setCoverUrlExpiresAt(Instant coverUrlExpiresAt) {
        this.coverUrlExpiresAt = coverUrlExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Course course)) {
            return false;
        }
        return Objects.equals(id, course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

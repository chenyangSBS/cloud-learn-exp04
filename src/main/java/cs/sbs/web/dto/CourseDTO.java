package cs.sbs.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CourseDTO {

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "标题最多200字符")
    private String title;

    @NotBlank(message = "讲师姓名不能为空")
    @Size(max = 100, message = "讲师名最多100字符")
    private String instructor;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @DecimalMax(value = "99999.99", message = "价格超出范围")
    private Double price;

    @Size(max = 2000, message = "描述最多2000字符")
    private String description;

    @NotNull(message = "课程时长不能为空")
    @Min(value = 1, message = "课程时长至少1小时")
    @Max(value = 1000, message = "课程时长超出范围")
    private Integer duration;

    @Min(value = 0, message = "学习人数不能为负数")
    private Integer studentCount = 0;

}

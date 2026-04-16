package cs.sbs.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllCourses_returnsSeededData() throws Exception {
        String body = mockMvc.perform(get("/api/courses").with(httpBasic("student", "password")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").isArray()).isTrue();
        assertThat(json.get("data").size()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void createCourse_invalidPayload_returns400WithFieldErrors() throws Exception {
        String body = mockMvc.perform(
                        post("/api/courses")
                                .with(httpBasic("student", "password"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "",
                                          "instructor": "Test",
                                          "price": -10,
                                          "duration": -5
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("errorCode").asInt()).isEqualTo(400);
        assertThat(json.get("data").get("title").asText()).contains("不能为空");
    }

    @Test
    void getCourseById_notFound_returns404() throws Exception {
        String body = mockMvc.perform(get("/api/courses/999999").with(httpBasic("student", "password")))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("errorCode").asInt()).isEqualTo(404);
    }
}

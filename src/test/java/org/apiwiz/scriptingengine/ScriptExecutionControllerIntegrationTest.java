package org.apiwiz.scriptingengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apiwiz.scriptingengine.dto.ScriptRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ScriptExecutionControllerIntegrationTest {
    private final static String path = "/api/scripts/execute";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testExecutePythonScript() throws Exception {
        ScriptRequest request = new ScriptRequest("python", "x = 42\nx");

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.output").value("42"));

    }

    @Test
    void testExecuteJavaScriptScript() throws Exception {
        ScriptRequest request = new ScriptRequest("js", "let y = 10 * 2; y");

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.output").value("20"));

    }
}

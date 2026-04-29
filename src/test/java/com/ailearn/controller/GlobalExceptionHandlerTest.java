package com.ailearn.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.http.HttpTimeoutException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void responseStatusException_shouldReturnUniformErrorBody() throws Exception {
        mockMvc().perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Learning article not found"))
                .andExpect(jsonPath("$.path").value("/missing"));
    }

    @Test
    void illegalStateException_shouldReturnBadGatewayForExternalFailure() throws Exception {
        mockMvc().perform(get("/external"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Failed to fetch article content"))
                .andExpect(jsonPath("$.path").value("/external"));
    }

    @Test
    void httpTimeoutCause_shouldReturnGatewayTimeout() throws Exception {
        mockMvc().perform(get("/timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.error").value("Gateway Timeout"))
                .andExpect(jsonPath("$.message").value("Request to upstream service timed out"))
                .andExpect(jsonPath("$.path").value("/timeout"));
    }

    @Test
    void unexpectedException_shouldReturnGenericInternalServerError() throws Exception {
        mockMvc().perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"))
                .andExpect(jsonPath("$.path").value("/unexpected"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @RestController
    static class FailingController {
        @GetMapping("/missing")
        void missing() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found");
        }

        @GetMapping("/external")
        void external() {
            throw new IllegalStateException("Failed to fetch article content");
        }

        @GetMapping("/timeout")
        void timeout() {
            throw new RuntimeException(new HttpTimeoutException("request timed out"));
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new RuntimeException("database exploded");
        }
    }
}

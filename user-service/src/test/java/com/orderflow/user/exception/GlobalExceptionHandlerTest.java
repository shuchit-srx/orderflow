package com.orderflow.user.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                new TestController()
                        )
                        .setControllerAdvice(
                                new GlobalExceptionHandler()
                        )
                        .build();
    }

    @Test
    void validationErrorShouldUseCommonErrorContract()
            throws Exception {

        mockMvc.perform(
                        post("/test/validation")
                                .header(
                                        "X-Request-Id",
                                        "user-request-123"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": ""
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "VALIDATION_ERROR"
                                )
                )
                .andExpect(
                        jsonPath("$.message").exists()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/test/validation"
                                )
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "user-request-123"
                                )
                );
    }

    @Test
    void malformedJsonShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/test/validation")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name":
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "MALFORMED_REQUEST"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/test/validation"
                                )
                );
    }

    @Test
    void missingHeaderShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/test/header")
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "MISSING_HEADER"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/test/header"
                                )
                );
    }

    @Test
    void unsupportedMethodShouldReturn405()
            throws Exception {

        mockMvc.perform(
                        get("/test/validation")
                )
                .andExpect(
                        status().isMethodNotAllowed()
                )
                .andExpect(
                        jsonPath("$.status").value(405)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "METHOD_NOT_ALLOWED"
                                )
                );
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validation")
        void validate(
                @Valid
                @RequestBody
                TestRequest request
        ) {
        }

        @GetMapping("/header")
        void requireHeader(
                @RequestHeader("X-Required")
                String requiredHeader
        ) {
        }
    }

    record TestRequest(
            @NotBlank(
                    message = "name must not be blank"
            )
            String name
    ) {
    }
}
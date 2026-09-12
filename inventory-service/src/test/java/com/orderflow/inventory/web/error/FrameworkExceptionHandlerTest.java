package com.orderflow.inventory.web.error;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrameworkExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                new TestController()
                        )
                        .setControllerAdvice(
                                new FrameworkExceptionHandler()
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
                                        "inventory-request-123"
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
                                        "inventory-request-123"
                                )
                );
    }

    @Test
    void malformedJsonShouldReturn400()
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
                        jsonPath("$.error")
                                .value(
                                        "MALFORMED_REQUEST"
                                )
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                );
    }

    @Test
    void invalidUuidParameterShouldReturn400()
            throws Exception {

        mockMvc.perform(
                        get("/test/parameter")
                                .param(
                                        "productId",
                                        "not-a-uuid"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_PARAMETER"
                                )
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/test/parameter"
                                )
                );
    }

    @Test
    void missingRequiredHeaderShouldReturn400()
            throws Exception {

        mockMvc.perform(
                        get("/test/header")
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "MISSING_HEADER"
                                )
                )
                .andExpect(
                        jsonPath("$.status").value(400)
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
                        jsonPath("$.error")
                                .value(
                                        "METHOD_NOT_ALLOWED"
                                )
                )
                .andExpect(
                        jsonPath("$.status").value(405)
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

        @GetMapping("/parameter")
        void parameter(
                @RequestParam
                UUID productId
        ) {
        }

        @GetMapping("/header")
        void header(
                @RequestHeader("X-Internal-Test")
                String header
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
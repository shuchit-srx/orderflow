package com.orderflow.order.web.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

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
    void validationErrorShouldUseCommonContract()
            throws Exception {

        mockMvc.perform(
                        post("/test/validation")
                                .header(
                                        "X-Request-Id",
                                        "order-request-123"
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
                        jsonPath("$.path")
                                .value(
                                        "/test/validation"
                                )
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "order-request-123"
                                )
                );
    }

    @Test
    void insufficientStockShouldRemain409()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/insufficient-stock"
                        )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status").value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INSUFFICIENT_STOCK"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Requested inventory is not available"
                                )
                );
    }

    @Test
    void genericInventoryConflictShouldReturn409()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/conflict"
                        )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVENTORY_CONFLICT"
                                )
                );
    }

    @Test
    void inventoryBadRequestShouldRemain400()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/bad-request"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_INVENTORY_REQUEST"
                                )
                );
    }

    @Test
    void missingInventoryProductShouldReturn400()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/not-found"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_ORDER_ITEM"
                                )
                );
    }

    @Test
    void inventory500ShouldBecome503()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/server-error"
                        )
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.status").value(503)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVENTORY_UNAVAILABLE"
                                )
                );
    }

    @Test
    void inventoryAuthenticationFailureShouldBecome503()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/unauthorized"
                        )
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVENTORY_UNAVAILABLE"
                                )
                );
    }

    @Test
    void inventoryConnectionFailureShouldBecome503()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/unavailable"
                        )
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.status").value(503)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVENTORY_UNAVAILABLE"
                                )
                );
    }

    @Test
    void inventoryTimeoutShouldBecome504()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/timeout"
                        )
                )
                .andExpect(
                        status().isGatewayTimeout()
                )
                .andExpect(
                        jsonPath("$.status").value(504)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVENTORY_TIMEOUT"
                                )
                );
    }

    @Test
    void unexpectedInventoryResponseShouldBecome502()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/inventory/unexpected"
                        )
                )
                .andExpect(
                        status().isBadGateway()
                )
                .andExpect(
                        jsonPath("$.status").value(502)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVENTORY_BAD_RESPONSE"
                                )
                );
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validation")
        void validation(
                @Valid
                @RequestBody
                TestRequest request
        ) {
        }

        @GetMapping("/inventory/insufficient-stock")
        void insufficientStock() {
            throw clientError(
                    HttpStatus.CONFLICT,
                    """
                    {
                      "error": "INSUFFICIENT_STOCK"
                    }
                    """
            );
        }

        @GetMapping("/inventory/conflict")
        void inventoryConflict() {
            throw clientError(
                    HttpStatus.CONFLICT,
                    """
                    {
                      "error": "RESERVATION_CONFLICT"
                    }
                    """
            );
        }

        @GetMapping("/inventory/bad-request")
        void inventoryBadRequest() {
            throw clientError(
                    HttpStatus.BAD_REQUEST,
                    """
                    {
                      "error": "VALIDATION_ERROR"
                    }
                    """
            );
        }

        @GetMapping("/inventory/not-found")
        void inventoryNotFound() {
            throw clientError(
                    HttpStatus.NOT_FOUND,
                    """
                    {
                      "error": "PRODUCT_NOT_FOUND"
                    }
                    """
            );
        }

        @GetMapping("/inventory/server-error")
        void inventoryServerError() {
            throw HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    new HttpHeaders(),
                    new byte[0],
                    StandardCharsets.UTF_8
            );
        }

        @GetMapping("/inventory/unauthorized")
        void inventoryUnauthorized() {
            throw clientError(
                    HttpStatus.UNAUTHORIZED,
                    """
                    {
                      "error": "UNAUTHORIZED"
                    }
                    """
            );
        }

        @GetMapping("/inventory/unavailable")
        void inventoryUnavailable() {
            throw new ResourceAccessException(
                    "Connection refused",
                    new ConnectException(
                            "Connection refused"
                    )
            );
        }

        @GetMapping("/inventory/timeout")
        void inventoryTimeout() {
            throw new ResourceAccessException(
                    "Read timed out",
                    new SocketTimeoutException(
                            "Read timed out"
                    )
            );
        }

        @GetMapping("/inventory/unexpected")
        void unexpectedInventoryResponse() {
            throw clientError(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    """
                    {
                      "error": "UNKNOWN"
                    }
                    """
            );
        }

        private HttpClientErrorException clientError(
                HttpStatus status,
                String body
        ) {
            return HttpClientErrorException.create(
                    status,
                    status.getReasonPhrase(),
                    new HttpHeaders(),
                    body.getBytes(
                            StandardCharsets.UTF_8
                    ),
                    StandardCharsets.UTF_8
            );
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
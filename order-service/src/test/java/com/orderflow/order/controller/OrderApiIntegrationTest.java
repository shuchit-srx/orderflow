package com.orderflow.order.controller;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;

import com.orderflow.order.service.OrderService;

import com.orderflow.order.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderApiIntegrationTest
        extends AbstractIntegrationTest {

    @SuppressWarnings(
            "SpringJavaInjectionPointsAutowiringInspection"
    )
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    private static final String CREATE_BODY =
            """
                    {
                      "items": [
                        {
                          "productId":
                            "11111111-1111-1111-1111-111111111111",
                          "quantity": 2
                        }
                      ]
                    }
            """;

    @Test
    void unauthenticatedUserCannotAccessOrders()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/orders")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void customerCanCreateOrder()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        UUID.randomUUID()
                                                .toString()
                                )
                                .with(
                                        jwt()
                                                .jwt(
                                                        token ->
                                                                token.subject(
                                                                                CUSTOMER_1.toString()
                                                                        )
                                                                        .claim(
                                                                                "role",
                                                                                "CUSTOMER"
                                                                        )
                                                )
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"
                                                        )
                                                )
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        CREATE_BODY
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.customerId"
                        )
                                .value(
                                        CUSTOMER_1.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CONFIRMED")
                )
                .andExpect(
                        jsonPath(
                                "$.totalAmount"
                        )
                                .value(139998.00)
                );
    }

    @Test
    void adminCannotUseCustomerOrderEndpoint()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        UUID.randomUUID()
                                                .toString()
                                )
                                .with(
                                        jwt()
                                                .jwt(
                                                        token ->
                                                                token.subject(
                                                                                CUSTOMER_1.toString()
                                                                        )
                                                                        .claim(
                                                                                "role",
                                                                                "ADMIN"
                                                                        )
                                                )
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_ADMIN"
                                                        )
                                                )
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        CREATE_BODY
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void customerCannotReadAnotherCustomersOrder()
            throws Exception {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_2,
                                createRequest()
                        )
                        .id();

        mockMvc.perform(
                        get(
                                "/api/v1/orders/{orderId}",
                                orderId
                        )

                                .with(
                                        customerJwt(
                                                CUSTOMER_1
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "ORDER_NOT_FOUND"
                                )
                );
    }

    @Test
    void customerListContainsOnlyOwnOrders()
            throws Exception {

        orderService.createOrder(
                CUSTOMER_1,
                createRequest()
        );

        orderService.createOrder(
                CUSTOMER_2,
                createRequest()
        );

        mockMvc.perform(
                        get("/api/v1/orders")

                                .with(
                                        customerJwt(
                                                CUSTOMER_1
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.totalElements"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].customerId"
                        )
                                .value(
                                        CUSTOMER_1.toString()
                                )
                );
    }

    @Test
    void adminCanUseInternalTransitionEndpoint()
            throws Exception {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                createRequest()
                        )
                        .id();

        mockMvc.perform(
                        post(
                                "/api/v1/internal/orders/{orderId}/inventory-reserved",
                                orderId
                        )

                                .with(
                                        adminJwt()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "INVENTORY_RESERVED"
                                )
                );
    }

    @Test
    void customerCannotUseInternalTransitionEndpoint()
            throws Exception {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                createRequest()
                        )
                        .id();

        mockMvc.perform(
                        post(
                                "/api/v1/internal/orders/{orderId}/inventory-reserved",
                                orderId
                        )

                                .with(
                                        customerJwt(
                                                CUSTOMER_1
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void invalidTransitionReturnsConflict()
            throws Exception {

        UUID orderId =
                orderService
                        .createOrder(
                                CUSTOMER_1,
                                createRequest()
                        )
                        .id();

        orderService
                .markInventoryReserved(
                        orderId
                );

        orderService
                .confirmOrder(
                        orderId
                );

        mockMvc.perform(
                        post(
                                "/api/v1/internal/orders/{orderId}/cancel",
                                orderId
                        )

                                .with(
                                        adminJwt()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_ORDER_STATE"
                                )
                );
    }

    @Test
    void emptyItemsShouldFailValidation()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/orders")
                                .header(
                                        "Idempotency-Key",
                                        UUID.randomUUID()
                                                .toString()
                                )
                                .with(
                                        customerJwt(
                                                CUSTOMER_1
                                        )
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        """
                                        {
                                          "items": []
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
                                        "VALIDATION_ERROR"
                                )
                );
    }

    @Test
    void missingIdempotencyKeyShouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(
                                        customerJwt(
                                                CUSTOMER_1
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "items":[
                                            {
                                              "productId":
                                                "11111111-1111-1111-1111-111111111111",
                                              "quantity":1
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status()
                                .isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_IDEMPOTENCY_KEY"
                                )
                );
    }

    private CreateOrderRequest createRequest() {

        return new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(
                                IPHONE_ID,
                                1
                        )
                )
        );
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    customerJwt(
            UUID customerId
    ) {

        return jwt()
                .jwt(
                        token ->
                                token.subject(
                                                customerId.toString()
                                        )
                                        .claim(
                                                "role",
                                                "CUSTOMER"
                                        )
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_CUSTOMER"
                        )
                );
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    adminJwt() {

        return jwt()
                .jwt(
                        token ->
                                token.subject(
                                                UUID.randomUUID()
                                                        .toString()
                                        )
                                        .claim(
                                                "role",
                                                "ADMIN"
                                        )
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_ADMIN"
                        )
                );
    }
}
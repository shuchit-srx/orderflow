package com.orderflow.inventory.controller;

import com.orderflow.inventory.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void productReadShouldBePublic()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/products/{id}",
                                IPHONE_ID
                        )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void inventoryReadShouldBePublic()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/inventory/{id}",
                                IPHONE_ID
                        )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void adminShouldAdjustInventory()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/admin/inventory/{id}/adjust",
                                IPHONE_ID
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_ADMIN"
                                                )
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "delta": 5
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.availableQuantity"
                        ).value(25)
                );
    }

    @Test
    void customerShouldNotAdjustInventory()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/admin/inventory/{id}/adjust",
                                IPHONE_ID
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_CUSTOMER"
                                                )
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "delta": 5
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void unauthenticatedUserShouldNotAdjustInventory()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/admin/inventory/{id}/adjust",
                                IPHONE_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "delta": 5
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void adminShouldCreateReservation()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/internal/inventory/reservations"
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_ADMIN"
                                                )
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "orderId":
                                            "91000000-0000-0000-0000-000000000001",
                                          "items": [
                                            {
                                              "productId":
                                                "11111111-1111-1111-1111-111111111111",
                                              "quantity": 5
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void customerShouldNotCallInternalReservationApi()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/internal/inventory/reservations"
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_CUSTOMER"
                                                )
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "orderId":
                                            "92000000-0000-0000-0000-000000000001",
                                          "items": [
                                            {
                                              "productId":
                                                "11111111-1111-1111-1111-111111111111",
                                              "quantity": 1
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void invalidReservationQuantityShouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/internal/inventory/reservations"
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_ADMIN"
                                                )
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "orderId":
                                            "93000000-0000-0000-0000-000000000001",
                                          "items": [
                                            {
                                              "productId":
                                                "11111111-1111-1111-1111-111111111111",
                                              "quantity": 0
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("VALIDATION_ERROR")
                );
    }

    @Test
    void insufficientStockShouldReturn409()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/internal/inventory/reservations"
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_ADMIN"
                                                )
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "orderId":
                                            "94000000-0000-0000-0000-000000000001",
                                          "items": [
                                            {
                                              "productId":
                                                "11111111-1111-1111-1111-111111111111",
                                              "quantity": 500
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INSUFFICIENT_STOCK"
                                )
                );
    }
}
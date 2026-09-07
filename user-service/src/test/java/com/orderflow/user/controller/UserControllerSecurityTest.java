package com.orderflow.user.controller;

import com.orderflow.user.config.JwtAuthenticationConverterConfig;
import com.orderflow.user.config.SecurityConfig;
import com.orderflow.user.config.security.RestAccessDeniedHandler;
import com.orderflow.user.config.security.RestAuthenticationEntryPoint;
import com.orderflow.user.domain.UserRole;
import com.orderflow.user.domain.UserStatus;
import com.orderflow.user.dto.user.UpdateUserRequest;
import com.orderflow.user.dto.user.UserResponse;
import com.orderflow.user.exception.GlobalExceptionHandler;
import com.orderflow.user.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        properties = {
                "spring.jackson.deserialization.fail-on-unknown-properties=true"
        }
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationConverterConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class UserControllerSecurityTest {

    private static final UUID USER_ID =
            UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void usersMeWithoutTokenShouldReturn401()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("UNAUTHORIZED")
                );
    }

    @Test
    void customerShouldAccessUsersMe()
            throws Exception {

        when(userService.getCurrentUser(USER_ID))
                .thenReturn(customerResponse("Test User"));

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .with(
                                        jwt()
                                                .jwt(builder ->
                                                        builder.subject(
                                                                USER_ID.toString()
                                                        )
                                                )
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"
                                                        )
                                                )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.email")
                                .value("test@example.com")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("CUSTOMER")
                );
    }

    @Test
    void customerShouldBeForbiddenFromAdminApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/test")
                                .with(
                                        jwt()
                                                .jwt(builder ->
                                                        builder.subject(
                                                                USER_ID.toString()
                                                        )
                                                )
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"
                                                        )
                                                )
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.error")
                                .value("FORBIDDEN")
                );
    }

    @Test
    void updateProfileShouldWorkWithJwt()
            throws Exception {

        when(userService.updateCurrentUser(
                eq(USER_ID),
                any(UpdateUserRequest.class)
        )).thenReturn(
                customerResponse("Updated User")
        );

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .with(
                                        jwt()
                                                .jwt(builder ->
                                                        builder.subject(
                                                                USER_ID.toString()
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
                                .content("""
                                        {
                                          "displayName": "Updated User"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Updated User")
                );
    }

    @Test
    void unknownJsonPropertyShouldReturn400()
            throws Exception {

        mockMvc.perform(
                        put("/api/v1/users/me")
                                .with(
                                        jwt()
                                                .jwt(builder ->
                                                        builder.subject(
                                                                USER_ID.toString()
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
                                .content("""
                                        {
                                          "displayName": "Test",
                                          "role": "ADMIN"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value("MALFORMED_REQUEST")
                );
    }

    @Test
    void invalidBearerTokenShouldReturn401()
            throws Exception {

        when(jwtDecoder.decode("bad-token"))
                .thenThrow(
                        new BadJwtException("Invalid JWT")
                );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer bad-token"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error")
                                .value("UNAUTHORIZED")
                );
    }

    private UserResponse customerResponse(
            String displayName
    ) {

        Instant now = Instant.now();

        return new UserResponse(
                USER_ID,
                "test@example.com",
                displayName,
                UserRole.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now
        );
    }
}
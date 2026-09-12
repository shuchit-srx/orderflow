package com.orderflow.inventory.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component("orderflowRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter
        extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER =
            "X-Request-Id";

    public static final String REQUEST_ID_MDC_KEY =
            "requestId";

    private static final Pattern REQUEST_ID_PATTERN =
            Pattern.compile(
                    "[A-Za-z0-9._:-]{1,128}"
            );

    private static final Logger log =
            LoggerFactory.getLogger(
                    RequestContextFilter.class
            );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId =
                resolveRequestId(
                        request.getHeader(
                                REQUEST_ID_HEADER
                        )
                );

        HttpServletRequest wrappedRequest =
                new RequestIdRequestWrapper(
                        request,
                        requestId
                );

        response.setHeader(
                REQUEST_ID_HEADER,
                requestId
        );

        MDC.put(
                REQUEST_ID_MDC_KEY,
                requestId
        );

        long startedAt =
                System.nanoTime();

        try {
            filterChain.doFilter(
                    wrappedRequest,
                    response
            );
        } finally {
            long durationMs =
                    (
                            System.nanoTime()
                                    - startedAt
                    )
                            / 1_000_000;

            log.info(
                    "operation=http_request method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );

            MDC.remove(
                    REQUEST_ID_MDC_KEY
            );
        }
    }

    private String resolveRequestId(
            String candidate
    ) {
        if (
                candidate != null
                        && REQUEST_ID_PATTERN
                        .matcher(
                                candidate.trim()
                        )
                        .matches()
        ) {
            return candidate.trim();
        }

        return UUID
                .randomUUID()
                .toString();
    }

    private static final class RequestIdRequestWrapper
            extends HttpServletRequestWrapper {

        private final String requestId;

        private RequestIdRequestWrapper(
                HttpServletRequest request,
                String requestId
        ) {
            super(request);

            this.requestId =
                    requestId;
        }

        @Override
        public String getHeader(
                String name
        ) {
            if (
                    REQUEST_ID_HEADER.equalsIgnoreCase(
                            name
                    )
            ) {
                return requestId;
            }

            return super.getHeader(
                    name
            );
        }

        @Override
        public Enumeration<String> getHeaders(
                String name
        ) {
            if (
                    REQUEST_ID_HEADER.equalsIgnoreCase(
                            name
                    )
            ) {
                return Collections.enumeration(
                        List.of(
                                requestId
                        )
                );
            }

            return super.getHeaders(
                    name
            );
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names =
                    new ArrayList<>();

            Enumeration<String> existing =
                    super.getHeaderNames();

            boolean requestIdPresent =
                    false;

            if (
                    existing != null
            ) {
                while (
                        existing.hasMoreElements()
                ) {
                    String name =
                            existing.nextElement();

                    names.add(
                            name
                    );

                    if (
                            REQUEST_ID_HEADER
                                    .equalsIgnoreCase(
                                            name
                                    )
                    ) {
                        requestIdPresent =
                                true;
                    }
                }
            }

            if (
                    !requestIdPresent
            ) {
                names.add(
                        REQUEST_ID_HEADER
                );
            }

            return Collections.enumeration(
                    names
            );
        }
    }
}
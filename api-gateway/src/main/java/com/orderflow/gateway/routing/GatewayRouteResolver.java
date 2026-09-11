package com.orderflow.gateway.routing;

import com.orderflow.gateway.config.ServiceEndpoints;
import com.orderflow.gateway.exception.RouteNotFoundException;

import org.springframework.stereotype.Component;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class GatewayRouteResolver {

    private final ServiceEndpoints endpoints;

    public GatewayRouteResolver(
            ServiceEndpoints endpoints
    ) {
        this.endpoints = endpoints;
    }

    public URI resolve(
            String path,
            String queryString
    ) {

        if (matches(
                path,
                "/api/v1/internal"
        )) {

            throw new RouteNotFoundException(
                    path
            );
        }

        URI baseUri;

        if (matches(path, "/api/v1/auth")
                || matches(
                path,
                "/api/v1/users"
        )) {

            baseUri =
                    endpoints.user();

        } else if (
                matches(
                        path,
                        "/api/v1/products"
                )
                        || matches(
                        path,
                        "/api/v1/inventory"
                )
                        || matches(
                        path,
                        "/api/v1/admin/inventory"
                )
        ) {

            baseUri =
                    endpoints.inventory();

        } else if (
                matches(
                        path,
                        "/api/v1/orders"
                )
        ) {

            baseUri =
                    endpoints.order();

        } else {

            throw new RouteNotFoundException(
                    path
            );
        }

        UriComponentsBuilder builder =
                UriComponentsBuilder
                        .fromUri(baseUri)
                        .path(path);

        if (queryString != null
                && !queryString.isBlank()) {

            builder.query(
                    queryString
            );
        }

        return builder
                .build(true)
                .toUri();
    }

    private boolean matches(
            String path,
            String prefix
    ) {

        return path.equals(prefix)
                || path.startsWith(
                prefix + "/"
        );
    }
}
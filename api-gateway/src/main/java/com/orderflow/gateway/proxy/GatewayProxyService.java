package com.orderflow.gateway.proxy;

import com.orderflow.gateway.exception.UpstreamUnavailableException;
import com.orderflow.gateway.routing.GatewayRouteResolver;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;

@Service
public class GatewayProxyService {

    private static final Set<String>
            HOP_BY_HOP_HEADERS =
            Set.of(
                    "connection",
                    "keep-alive",
                    "proxy-authenticate",
                    "proxy-authorization",
                    "te",
                    "trailer",
                    "transfer-encoding",
                    "upgrade",
                    "host",
                    "content-length"
            );

    private final RestClient restClient;

    private final GatewayRouteResolver
            routeResolver;

    public GatewayProxyService(
            RestClient restClient,
            GatewayRouteResolver routeResolver
    ) {

        this.restClient =
                restClient;

        this.routeResolver =
                routeResolver;
    }

    public ResponseEntity<byte[]> forward(
            HttpServletRequest request
    ) throws IOException {

        URI target =
                routeResolver.resolve(
                        request.getRequestURI(),
                        request.getQueryString()
                );

        HttpMethod method =
                HttpMethod.valueOf(
                        request.getMethod()
                );

        byte[] requestBody =
                request.getInputStream()
                        .readAllBytes();

        try {

            RestClient.RequestBodySpec spec =
                    restClient
                            .method(method)
                            .uri(target);

            spec.headers(
                    headers ->
                            copyRequestHeaders(
                                    request,
                                    headers
                            )
            );

            RestClient.RequestHeadersSpec<?>
                    finalSpec;

            if (requestBody.length > 0) {

                finalSpec =
                        spec.body(
                                requestBody
                        );

            } else {

                finalSpec = spec;
            }

            return finalSpec.exchange(
                    (clientRequest,
                     clientResponse) -> {

                        HttpHeaders responseHeaders =
                                new HttpHeaders();

                        clientResponse
                                .getHeaders()
                                .forEach(
                                        (name, values) -> {

                                            if (!isHopByHop(
                                                    name
                                            )) {

                                                responseHeaders
                                                        .put(
                                                                name,
                                                                values
                                                        );
                                            }
                                        }
                                );

                        byte[] body =
                                clientResponse
                                        .getBody()
                                        .readAllBytes();

                        return new ResponseEntity<>(
                                body,
                                responseHeaders,
                                clientResponse
                                        .getStatusCode()
                        );
                    }
            );

        } catch (ResourceAccessException exception) {

            throw new UpstreamUnavailableException(
                    "Downstream service is unavailable",
                    exception
            );
        }
    }

    private void copyRequestHeaders(
            HttpServletRequest request,
            HttpHeaders targetHeaders
    ) {

        Enumeration<String> names =
                request.getHeaderNames();

        while (names.hasMoreElements()) {

            String name =
                    names.nextElement();

            if (isHopByHop(name)) {
                continue;
            }

            Enumeration<String> values =
                    request.getHeaders(name);

            while (values.hasMoreElements()) {

                targetHeaders.add(
                        name,
                        values.nextElement()
                );
            }
        }
    }

    private boolean isHopByHop(
            String headerName
    ) {

        return HOP_BY_HOP_HEADERS
                .contains(
                        headerName
                                .toLowerCase(
                                        Locale.ROOT
                                )
                );
    }
}
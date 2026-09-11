package com.orderflow.order.client;

import com.orderflow.order.client.dto.InventoryProductResponse;
import com.orderflow.order.client.dto.ReserveInventoryRequest;

import com.orderflow.order.config.InventoryClientProperties;

import com.orderflow.order.exception.InventoryReservationRejectedException;
import com.orderflow.order.exception.InventoryServiceException;
import com.orderflow.order.exception.InventoryServiceUnavailableException;
import com.orderflow.order.exception.ProductUnavailableException;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.http.MediaType;

import org.springframework.stereotype.Component;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class RestInventoryClient
        implements InventoryClient {

    private static final String INTERNAL_TOKEN_HEADER =
            "X-Internal-Service-Token";

    private final RestClient restClient;

    private final String internalToken;

    public RestInventoryClient(
            @Qualifier("inventoryRestClient")
            RestClient restClient,
            InventoryClientProperties properties
    ) {

        this.restClient =
                restClient;

        this.internalToken =
                properties.internalToken();
    }

    @Override
    public InventoryProductResponse getProduct(
            UUID productId
    ) {

        try {

            InventoryProductResponse response =
                    restClient
                            .get()
                            .uri(
                                    "/api/v1/products/{productId}",
                                    productId
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .retrieve()
                            .body(
                                    InventoryProductResponse.class
                            );

            if (response == null) {

                throw new InventoryServiceException(
                        "Inventory Service returned an empty product response",
                        null
                );
            }

            return response;

        } catch (
                RestClientResponseException exception
        ) {

            if (
                    exception
                            .getStatusCode()
                            .value() == 404
            ) {

                throw new ProductUnavailableException(
                        productId
                );
            }

            if (
                    exception
                            .getStatusCode()
                            .is5xxServerError()
            ) {

                throw new InventoryServiceUnavailableException(
                        exception
                );
            }

            throw new InventoryServiceException(
                    "Inventory Service returned HTTP "
                            + exception
                            .getStatusCode()
                            .value(),
                    exception
            );

        } catch (
                ResourceAccessException exception
        ) {

            throw new InventoryServiceUnavailableException(
                    exception
            );
        }
    }

    @Override
    public void reserveInventory(
            ReserveInventoryRequest request
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            "/api/v1/internal/inventory/reservations"
                    )
                    .header(
                            INTERNAL_TOKEN_HEADER,
                            internalToken
                    )
                    .contentType(
                            MediaType.APPLICATION_JSON
                    )
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

        } catch (
                RestClientResponseException exception
        ) {

            int status =
                    exception
                            .getStatusCode()
                            .value();

            if (
                    status == 404
                            || status == 409
                            || status == 422
            ) {

                throw new InventoryReservationRejectedException(
                        request.orderId()
                );
            }

            if (
                    status == 401
                            || status == 403
            ) {

                throw new InventoryServiceException(
                        "Inventory Service rejected internal authentication",
                        exception
                );
            }

            if (
                    exception
                            .getStatusCode()
                            .is5xxServerError()
            ) {

                throw new InventoryServiceUnavailableException(
                        exception
                );
            }

            throw new InventoryServiceException(
                    "Inventory reservation failed with HTTP "
                            + status,
                    exception
            );

        } catch (
                ResourceAccessException exception
        ) {

            throw new InventoryServiceUnavailableException(
                    exception
            );
        }
    }

    @Override
    public void confirmReservation(
            UUID orderId
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            "/api/v1/internal/inventory/reservations/{orderId}/confirm",
                            orderId
                    )
                    .header(
                            INTERNAL_TOKEN_HEADER,
                            internalToken
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (
                RestClientResponseException exception
        ) {

            if (
                    exception
                            .getStatusCode()
                            .is5xxServerError()
            ) {

                throw new InventoryServiceUnavailableException(
                        exception
                );
            }

            throw new InventoryServiceException(
                    "Could not confirm inventory reservation for order "
                            + orderId
                            + ". HTTP "
                            + exception
                            .getStatusCode()
                            .value(),
                    exception
            );

        } catch (
                ResourceAccessException exception
        ) {

            throw new InventoryServiceUnavailableException(
                    exception
            );
        }
    }

    @Override
    public void releaseReservation(
            UUID orderId
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            "/api/v1/internal/inventory/reservations/{orderId}/release",
                            orderId
                    )
                    .header(
                            INTERNAL_TOKEN_HEADER,
                            internalToken
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (
                RestClientResponseException exception
        ) {

            if (
                    exception
                            .getStatusCode()
                            .value() == 404
            ) {

                return;
            }

            if (
                    exception
                            .getStatusCode()
                            .is5xxServerError()
            ) {

                throw new InventoryServiceUnavailableException(
                        exception
                );
            }

            throw new InventoryServiceException(
                    "Could not release inventory reservation for order "
                            + orderId
                            + ". HTTP "
                            + exception
                            .getStatusCode()
                            .value(),
                    exception
            );

        } catch (
                ResourceAccessException exception
        ) {

            throw new InventoryServiceUnavailableException(
                    exception
            );
        }
    }
}
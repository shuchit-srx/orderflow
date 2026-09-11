package com.orderflow.order.client;

import com.orderflow.order.client.dto.InventoryProductResponse;

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

    private final RestClient restClient;

    public RestInventoryClient(
            @Qualifier("inventoryRestClient")
            RestClient restClient
    ) {

        this.restClient =
                restClient;
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

            if (exception
                    .getStatusCode()
                    .value() == 404) {

                throw new ProductUnavailableException(
                        productId
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
}
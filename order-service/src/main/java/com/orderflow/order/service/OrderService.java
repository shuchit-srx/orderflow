package com.orderflow.order.service;

import com.orderflow.order.client.InventoryClient;
import com.orderflow.order.client.dto.InventoryProductResponse;

import com.orderflow.order.domain.Order;

import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import com.orderflow.order.exception.DuplicateProductException;
import com.orderflow.order.exception.IdempotencyKeyConflictException;
import com.orderflow.order.exception.InvalidIdempotencyKeyException;
import com.orderflow.order.exception.OrderNotFoundException;

import com.orderflow.order.idempotency.OrderRequestHasher;

import com.orderflow.order.repository.OrderRepository;

import com.orderflow.order.service.model.OrderCreationResult;
import com.orderflow.order.service.model.ResolvedOrderItem;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderPersistenceService
            orderPersistenceService;

    private final InventoryClient
            inventoryClient;

    private final OrderRequestHasher
            orderRequestHasher;

    public OrderService(
            OrderRepository orderRepository,
            OrderPersistenceService orderPersistenceService,
            InventoryClient inventoryClient,
            OrderRequestHasher orderRequestHasher
    ) {

        this.orderRepository =
                orderRepository;

        this.orderPersistenceService =
                orderPersistenceService;

        this.inventoryClient =
                inventoryClient;

        this.orderRequestHasher =
                orderRequestHasher;
    }

    /*
     * Existing non-idempotent creation method.
     *
     * Keep this because older integration tests
     * and internal test setup already use it.
     *
     * Public POST /orders should use
     * createOrderIdempotent(...) through
     * OrderSagaService.
     */
    public OrderResponse createOrder(
            UUID customerId,
            CreateOrderRequest request
    ) {

        validateNoDuplicateProducts(
                request
        );

        List<ResolvedOrderItem> resolvedItems =
                resolveItems(
                        request
                );

        return orderPersistenceService
                .create(
                        customerId,null,null,
                        resolvedItems
                );
    }

    /*
     * Phase 12 public order-creation path.
     *
     * Same customer + same Idempotency-Key
     * must never create multiple orders.
     */
    public OrderCreationResult createOrderIdempotent(
            UUID customerId,
            String idempotencyKey,
            CreateOrderRequest request
    ) {

        validateIdempotencyKey(
                idempotencyKey
        );

        validateNoDuplicateProducts(
                request
        );

        String requestHash =
                orderRequestHasher
                        .hash(
                                request
                        );

        /*
         * Fast replay path.
         *
         * If the order already exists, don't make
         * another Inventory Service request and
         * don't create another order.
         */
        var existingOrder =
                orderRepository
                        .findByCustomerIdAndIdempotencyKey(
                                customerId,
                                idempotencyKey
                        );

        if (
                existingOrder.isPresent()
        ) {

            return replayExisting(
                    existingOrder.get(),
                    requestHash,
                    idempotencyKey
            );
        }

        /*
         * Resolve trusted product information
         * from Inventory Service before persisting:
         *
         * - SKU
         * - name
         * - price
         *
         * Client only supplies:
         *
         * - productId
         * - quantity
         */
        List<ResolvedOrderItem> resolvedItems =
                resolveItems(
                        request
                );

        try {

            OrderResponse createdOrder =
                    orderPersistenceService
                            .createIdempotent(
                                    customerId,
                                    idempotencyKey,
                                    requestHash,
                                    resolvedItems
                            );

            return OrderCreationResult
                    .created(
                            createdOrder
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {

            /*
             * Race condition:
             *
             * Request A:
             * SELECT -> no order
             *
             * Request B:
             * SELECT -> no order
             *
             * Both try INSERT.
             *
             * UNIQUE(customer_id, idempotency_key)
             * allows only one winner.
             *
             * The loser comes here and replays
             * the order created by the winner.
             */
            Order existing =
                    orderRepository
                            .findByCustomerIdAndIdempotencyKey(
                                    customerId,
                                    idempotencyKey
                            )
                            .orElseThrow(
                                    () -> exception
                            );

            return replayExisting(
                    existing,
                    requestHash,
                    idempotencyKey
            );
        }
    }

    /*
     * Customer can only retrieve their own order.
     *
     * Another customer's order appears as 404,
     * not 403.
     */
    @Transactional(
            readOnly = true
    )
    public OrderResponse getOrder(
            UUID customerId,
            UUID orderId
    ) {

        Order order =
                orderRepository
                        .findByIdAndCustomerId(
                                orderId,
                                customerId
                        )
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                orderId
                                        )
                        );

        return OrderResponse.from(
                order
        );
    }

    /*
     * Customer order history.
     */
    @Transactional(
            readOnly = true
    )
    public Page<OrderResponse> getOrders(
            UUID customerId,
            Pageable pageable
    ) {

        return orderRepository
                .findByCustomerId(
                        customerId,
                        pageable
                )
                .map(
                        OrderResponse::from
                );
    }

    /*
     * CREATED -> INVENTORY_RESERVED
     */
    @Transactional
    public OrderResponse markInventoryReserved(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(
                        orderId
                );

        order.markInventoryReserved();

        return OrderResponse.from(
                order
        );
    }

    /*
     * INVENTORY_RESERVED -> CONFIRMED
     *
     * Phase 9 normal saga confirmation should
     * now go through OrderFinalizationService
     * because that creates the transactional
     * outbox row.
     *
     * Keep this method because existing state
     * transition tests/internal code may use it.
     */
    @Transactional
    public OrderResponse confirmOrder(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(
                        orderId
                );

        order.confirm();

        return OrderResponse.from(
                order
        );
    }

    /*
     * Allowed states are enforced by Order itself.
     */
    @Transactional
    public OrderResponse cancelOrder(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(
                        orderId
                );

        order.cancel();

        return OrderResponse.from(
                order
        );
    }

    /*
     * CREATED / INVENTORY_RESERVED -> FAILED
     */
    @Transactional
    public OrderResponse failOrder(
            UUID orderId
    ) {

        Order order =
                getOrderForUpdate(
                        orderId
                );

        order.fail();

        return OrderResponse.from(
                order
        );
    }

    /*
     * Resolve product data from Inventory Service.
     *
     * We never trust SKU/name/price from the client.
     */
    private List<ResolvedOrderItem> resolveItems(
            CreateOrderRequest request
    ) {

        return request
                .items()
                .stream()
                .map(
                        this::resolveItem
                )
                .toList();
    }

    private ResolvedOrderItem resolveItem(
            CreateOrderItemRequest item
    ) {

        InventoryProductResponse product =
                inventoryClient
                        .getProduct(
                                item.productId()
                        );

        return new ResolvedOrderItem(
                product.id(),
                product.sku(),
                product.name(),
                product.price(),
                item.quantity()
        );
    }

    /*
     * The same product must not occur multiple
     * times inside one CreateOrderRequest.
     */
    private void validateNoDuplicateProducts(
            CreateOrderRequest request
    ) {

        Set<UUID> productIds =
                new HashSet<>();

        for (
                CreateOrderItemRequest item :
                request.items()
        ) {

            boolean inserted =
                    productIds.add(
                            item.productId()
                    );

            if (!inserted) {

                throw new DuplicateProductException(
                        item.productId()
                );
            }
        }
    }

    /*
     * Existing key + same payload:
     * return original order.
     *
     * Existing key + different payload:
     * 409 conflict.
     */
    private OrderCreationResult replayExisting(
            Order existingOrder,
            String incomingRequestHash,
            String idempotencyKey
    ) {

        if (
                existingOrder.getRequestHash()
                        == null
        ) {

            /*
             * This should only matter for old
             * pre-Phase-12 orders.
             */
            throw new IdempotencyKeyConflictException(
                    idempotencyKey
            );
        }

        if (
                !existingOrder
                        .getRequestHash()
                        .equals(
                                incomingRequestHash
                        )
        ) {

            throw new IdempotencyKeyConflictException(
                    idempotencyKey
            );
        }

        return OrderCreationResult
                .replay(
                        OrderResponse.from(
                                existingOrder
                        )
                );
    }

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {

        if (
                idempotencyKey == null
                        || idempotencyKey.isBlank()
        ) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key header is required"
            );
        }

        if (
                idempotencyKey.length()
                        > 200
        ) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not exceed 200 characters"
            );
        }
    }

    /*
     * State-transition methods use a pessimistic
     * DB lock so two transitions cannot mutate
     * the same order concurrently.
     */
    private Order getOrderForUpdate(
            UUID orderId
    ) {

        return orderRepository
                .findByIdForUpdate(
                        orderId
                )
                .orElseThrow(
                        () ->
                                new OrderNotFoundException(
                                        orderId
                                )
                );
    }
}
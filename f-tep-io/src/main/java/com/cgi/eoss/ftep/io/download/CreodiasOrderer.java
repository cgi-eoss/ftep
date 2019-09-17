package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.io.ServiceIoException;
import com.cgi.eoss.ftep.logging.Logging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.log4j.Log4j2;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.CloseableThreadContext;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Log4j2
public class CreodiasOrderer {

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final KeyCloakTokenGenerator keyCloakTokenGenerator;

    private final int ORDER_WAIT_MINUTES = 1;
    private final int ORDER_WAIT_ITERATIONS = 576;
    private final int NOTIFY_USER_PROGRESS_ITERATIONS = 10;

    public CreodiasOrderer(OkHttpClient httpClient, KeyCloakTokenGenerator keyCloakTokenGenerator) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
        this.keyCloakTokenGenerator = keyCloakTokenGenerator;
    }

    /**
     * Order the product and halt the process until the order has completed
     * @param uri
     * @param orderUrl
     */
    public void orderProduct(URI uri, HttpUrl orderUrl) {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        String productIdFull = uri.getPath().substring(1);

        // Query all placed orders
        List<Order> placedOrders = getOrders(orderUrl);

        // Only place a new order if this product has not already been ordered
        // TODO: add any order status conditions as well, e.g. still place an order if the name matches a cancelled order?
        if (placedOrders.stream().noneMatch(ord -> ord.getOrderName().equals(productIdFull))) {

            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                LOG.info("Ordering product " + productIdFull + " from CREODIAS");
            }

            Order order = placeOrder(orderUrl, productIdFull);
            int orderId = order.getId();
            LOG.info("Order " + orderId + " for product " + productIdFull + " has been placed");

            // Notify users every N iterations
            AtomicInteger userNotifyCountdown = new AtomicInteger(NOTIFY_USER_PROGRESS_ITERATIONS);

            RetryPolicy retryPolicy = new RetryPolicy()
                    .withDelay(Duration.ofMinutes(ORDER_WAIT_MINUTES))
                    .withMaxRetries(ORDER_WAIT_ITERATIONS)
                    .withMaxDuration(Duration.ofMinutes(ORDER_WAIT_ITERATIONS * ORDER_WAIT_MINUTES))
                    .onRetry(e -> {
                        if (userNotifyCountdown.getAndDecrement() < 1) {
                            userNotifyCountdown.set(NOTIFY_USER_PROGRESS_ITERATIONS);
                            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                                LOG.info("Product order for " + productIdFull + " from CREODIAS is still in progress...");
                            }
                        }
                    })
                    .onRetriesExceeded(e -> {
                        LOG.error("Order " + orderId + " for product " + productIdFull + " timed out");
                        throw new ServiceIoException("Order " + orderId + " for product " + productIdFull + " timed out");
                    });

            Failsafe.with(retryPolicy)
                    .onSuccess(i -> {
                        try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                            LOG.info("Product order for " + productIdFull + " from CREODIAS has completed, continuing");
                        }
                    })
                    .run(() -> checkOrderArrival(orderUrl, orderId, productIdFull));

            // TODO: cancel the order if it times out?
        } else {
            LOG.info("Product " + productIdFull + " has already been ordered, skipping this");
        }
    }

    /**
     * Check whether a specific order has completed
     * @param orderUrl
     * @param orderId
     * @param productIdFull
     */
    public void checkOrderArrival(HttpUrl orderUrl, int orderId, String productIdFull) {
        List<OrderItem> orderItems = getOrderDetails(orderUrl, orderId);
        if (orderItems.size() < 1) {
            LOG.error("Failed to retrieve order details for CREODIAS order with ID " + orderId);
            throw new ServiceIoException("Failed to retrieve order details for CREODIAS order with ID " + orderId);
        }

        String status = orderItems.get(0).getStatus();
        if (status.equals("done")) {
            LOG.info("The order " + orderId + " for product " + productIdFull + " has completed.");
            return;
        } else if (ImmutableSet.of("not_found", "not_valid", "failed").contains(status)) {
            // TODO: add "removed_from_cache", "removing_from_cache", "scheduled_for_deletion" here as well?
            LOG.error("The order " + orderId + " for product " + productIdFull + " is not available with the status " + status);
            throw new ServiceIoException("The order " + orderId + " for product " + productIdFull + " is not available with the status " + status);
        }

        LOG.info("Order " + orderId + " for " + productIdFull + " has not completed yet, status: " + status);
        throw new ServiceIoException("Order " + orderId + " for " + productIdFull + " has not completed yet, status: " + status);
    }

    /**
     * A function for building and executing a GET request to CREODIAS to retrieve all placed orders
     * @param orderUrl
     * @return
     */
    private List<Order> getOrders(HttpUrl orderUrl) {

        List<Order> orders = new ArrayList<>();

        int orderCount = getOrderCount(orderUrl);

        // Orders are paginated by 10 - collect and return orders from all pages
        for (int pageCounter = 1; pageCounter < (orderCount + 9) / 10 + 1; pageCounter++) {

            String token = getKeyCloakAuthenticationToken(orderUrl);

            // Build the GET request for retrieving all orders from the corresponding page
            Request request = new Request.Builder()
                    .url(orderUrl.newBuilder()
                            .addQueryParameter("page", Integer.toString(pageCounter))
                            .build())
                    .addHeader("Content-Type", APPLICATION_JSON_VALUE)
                    .addHeader("Keycloak-Token", token)
                    .build();

            // Execute the call
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOG.error("Received unsuccessful HTTP response for CREODIAS page orders search: {}", response.toString());
                    throw new ServiceIoException("Unexpected HTTP response from CREODIAS: " + response.message());
                }

                String responseBody = response.body().string();

                List<Map<String, Object>> results = JsonPath.read(responseBody, "$.results");
                List<Order> pageOrders = results.stream()
                        .map(result -> objectMapper.convertValue(result, Order.class))
                        .collect(Collectors.toList());
                orders.addAll(pageOrders);
            } catch (IOException e) {
                LOG.error("Error when requesting the order list from CREODIAS: " + e.getMessage());
                throw new ServiceIoException("Error when requesting the order list from CREODIAS: " + e.getMessage());
            }

        }
        return orders;
    }

    /**
     * Retrieve the total number of orders
     * @param orderUrl
     * @return
     */
    private int getOrderCount(HttpUrl orderUrl) {

        String token = getKeyCloakAuthenticationToken(orderUrl);

        // Build the GET request for retrieving all orders
        Request request = new Request.Builder()
                .url(orderUrl)
                .addHeader("Content-Type", APPLICATION_JSON_VALUE)
                .addHeader("Keycloak-Token", token)
                .build();

        // Execute the call
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CREODIAS orders search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CREODIAS: " + response.message());
            }

            String responseBody = response.body().string();
            int orderCount = JsonPath.read(responseBody, "$.count");
            return orderCount;
        } catch (IOException e) {
            LOG.error("Error when retrieving the total order count from CREODIAS: " + e.getMessage());
            throw new ServiceIoException("Error when retrieving the total order count from CREODIAS: " + e.getMessage());
        }
    }



    /**
     * A function for placing an order for a product by sending a POST request to CREODIAS
     * @param orderUrl
     */
    private Order placeOrder(HttpUrl orderUrl, String productIdFull) {

        String token = getKeyCloakAuthenticationToken(orderUrl);

        Order order = new Order();
        order.setPriority(1);
        order.setOrderName(productIdFull);
        order.setProcessor("sen2cor");
        order.setIdentifierList(Arrays.asList(productIdFull));

        RequestBody requestBody;
        try {
            requestBody = RequestBody.create(MediaType.parse(APPLICATION_JSON_VALUE), objectMapper.writeValueAsString(order));
        } catch (JsonProcessingException e) {
            LOG.error("Error converting an order to JSON: " + e.getMessage());
            throw new ServiceIoException("Error converting an order to JSON");
        }
        // Build the POST request for placing an order
        Request request = new Request.Builder()
                .url(orderUrl)
                .addHeader("Content-Type", APPLICATION_JSON_VALUE)
                .addHeader("Keycloak-Token", token)
                .post(requestBody)
                .build();

        // Execute the call
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CREODIAS order: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CREODIAS: " + response.message());
            }
            return objectMapper.readValue(response.body().string(), Order.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceIoException("Error when placing an order to CREODIAS for product " + orderUrl + ": " + e.getMessage());
        }
    }

    private List<OrderItem> getOrderDetails(HttpUrl orderUrl, Integer orderId) {

        String token = getKeyCloakAuthenticationToken(orderUrl);

        HttpUrl orderDetailsUrl = orderUrl.newBuilder()
                .addPathSegment(orderId.toString())
                .addPathSegment("order_items")
                .build();

        // Build the GET request for retrieving the details for an order
        Request request = new Request.Builder()
                .url(orderDetailsUrl)
                .addHeader("Content-Type", APPLICATION_JSON_VALUE)
                .addHeader("Keycloak-Token", token)
                .build();

        // Execute the call
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CREODIAS order details search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CREODIAS: " + response.message());
            }
            String responseBody = response.body().string();

            List<Map<String, Object>> results = JsonPath.read(responseBody, "$.results");
            return results.stream()
                    .map(res -> objectMapper.convertValue(res, OrderItem.class))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceIoException("Error when requesting the order details from CREODIAS for order " + orderId + ": " + e.getMessage());
        }
    }

    /**
     * Retrieving the KeyCloak authentication token
     * @param url
     * @return
     */
    private String getKeyCloakAuthenticationToken(HttpUrl url) {
        return keyCloakTokenGenerator.getKeyCloakAuthenticationToken(url).getAccessToken().getTokenValue();
    }

}

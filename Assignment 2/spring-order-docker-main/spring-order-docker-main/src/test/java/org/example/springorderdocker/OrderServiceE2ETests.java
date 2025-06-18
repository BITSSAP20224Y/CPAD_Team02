package org.example.springorderdocker;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderServiceE2ETests {

  private final String BASE_URL = "http://localhost:9092/order";
  private final RestTemplate restTemplate = new RestTemplate();

  @Test
  public void testGetAllOrders() {
    ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/findAll", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("orderId"));
  }

  @Test
  public void testGetOrderById_Valid() {
    ResponseEntity<Map> response = restTemplate.getForEntity(BASE_URL + "/1", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().get("orderId"));
  }

  @Test
  public void testGetOrderById_NotFound() throws JsonProcessingException {
    // Ensure the order with ID 171 does not exist
    restTemplate.delete(BASE_URL + "/171");

    try {
      ResponseEntity<Map> response = restTemplate.getForEntity(BASE_URL + "/171", Map.class);
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
      Map<String, Object> errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
      assertEquals(404, errorResponse.get("error"));
      assertTrue(errorResponse.get("message").toString().contains("Order not found with id: 171"));
    }
  }

  @Test
  public void testInsertOrder_Success() {
    Map<String, Object> order = Map.of("productId", 4, "quantity", 5);
    ResponseEntity<Map> response = restTemplate.postForEntity(BASE_URL + "/insert", order, Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(4, response.getBody().get("productId"));
  }

  @Test
  public void testInsertOrder_ProductNotFound() throws JsonProcessingException {
    Map<String, Object> order = Map.of("productId", 222, "quantity", 25);

    try {
      restTemplate.postForEntity(BASE_URL + "/insert", order, Map.class);
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Map<String, Object> errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
      assertEquals(400, errorResponse.get("error"));
      assertTrue(errorResponse.get("message").toString().contains("Product not found with id: 222"));
    }
  }

  @Test
  public void testInsertOrder_InsufficientInventory() throws JsonProcessingException {
    Map<String, Object> order = Map.of("productId", 2, "quantity", 253333);

    try {
      restTemplate.postForEntity(BASE_URL + "/insert", order, Map.class);
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Map<String, Object> errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
      assertEquals(400, errorResponse.get("error"));
      assertTrue(errorResponse.get("message").toString().contains("Insufficient inventory"));
    }
  }

  @Test
  public void testUpdateOrder_Success() {
    Map<String, Object> order = Map.of(
        "orderId", 1,
        "productId", 3,
        "quantity", 5,
        "orderDate", "2025-05-15T06:58:27.397+00:00",
        "totalAmount", 1600,
        "status", "CONFIRMED",
        "createdAt", "2025-05-15T06:58:27.397+00:00"
    );
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(order);
    ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/1", HttpMethod.PUT, request, Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().get("orderId"));
  }

  @Test
  public void testUpdateOrder_NotFound() throws JsonProcessingException {
    Map<String, Object> order = Map.of(
        "orderId", 99999,
        "productId", 3,
        "quantity", 5,
        "orderDate", "2025-05-15T06:58:27.397+00:00",
        "totalAmount", 1600,
        "status", "CONFIRMED",
        "createdAt", "2025-05-15T06:58:27.397+00:00"
    );
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(order);

    try {
      restTemplate.exchange(BASE_URL + "/99999", HttpMethod.PUT, request, Map.class);
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
      Map<String, Object> errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
      assertEquals(404, errorResponse.get("error"));
      assertTrue(errorResponse.get("message").toString().contains("Order not found with id: 99999"));
    }
  }

  @Test
  public void testDeleteOrder() throws JsonProcessingException {
    // Insert a new order first
    Map<String, Object> newOrder = Map.of("productId", 4, "quantity", 5);
    ResponseEntity<Map> insertResponse = restTemplate.postForEntity(BASE_URL + "/insert", newOrder, Map.class);
    assertEquals(HttpStatus.OK, insertResponse.getStatusCode());
    Integer orderId = (Integer) insertResponse.getBody().get("orderId");

    // Now delete it
    restTemplate.delete(BASE_URL + "/" + orderId);

    // Confirm it's deleted
    try {
      ResponseEntity<Map> getResponse = restTemplate.getForEntity(BASE_URL + "/" + orderId, Map.class);
      assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
      Map<String, Object> errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
      assertEquals(404, errorResponse.get("error"));
      assertTrue(errorResponse.get("message").toString().contains("Order not found with id: " + orderId));
    }
  }
}

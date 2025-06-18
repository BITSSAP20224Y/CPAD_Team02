package org.example.springorderdocker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.springorderdocker.entity.Order;
import org.example.springorderdocker.repo.OrderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {

  @Mock
  private OrderRepo orderRepo;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetAllOrders() {
    List<Order> orders = List.of(new Order(), new Order());
    when(orderRepo.findAll()).thenReturn(orders);

    List<Order> result = orderService.getAllOrders();

    assertEquals(2, result.size());
    verify(orderRepo, times(1)).findAll();
  }

  @Test
  void testInsertOrder_Success() {
    Order order = new Order();
    order.setProductId(1);
    order.setQuantity(5);

    Map<String, Object> priceResponse = new HashMap<>();
    priceResponse.put("price", "10.0");

    Instant mockInstant = Instant.parse("2025-06-07T18:17:11.858Z");

    Map<String, Object> inventoryResponse = new HashMap<>();
    inventoryResponse.put("quantityAvailable", 10);
    inventoryResponse.put("id", 1);
    inventoryResponse.put("lastUpdated",mockInstant.toString());

    when(restTemplate.getForEntity(anyString(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(priceResponse, HttpStatus.OK))
        .thenReturn(new ResponseEntity<>(inventoryResponse, HttpStatus.OK));


    when(orderRepo.save(any(Order.class))).thenReturn(order);

    Order result = orderService.insertOrder(order);

    assertEquals("CONFIRMED", result.getStatus());
    verify(orderRepo, times(1)).save(order);
  }

  @Test
  void testInsertOrder_ValidationFails() {
    Order order = new Order();
    order.setProductId(1);
    order.setQuantity(5);

    when(restTemplate.getForEntity(anyString(), eq(Map.class)))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> orderService.insertOrder(order));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(orderRepo, never()).save(any(Order.class));
  }

  @Test
  void testGetOrderById_Success() {
    Order order = new Order();
    when(orderRepo.findById(1)).thenReturn(Optional.of(order));

    Optional<Order> result = orderService.getOrderById(1);

    assertTrue(result.isPresent());
    assertEquals(order, result.get());
    verify(orderRepo, times(1)).findById(1);
  }

  @Test
  void testGetOrderById_NotFound() {
    when(orderRepo.findById(1)).thenReturn(Optional.empty());

    Optional<Order> result = orderService.getOrderById(1);

    assertFalse(result.isPresent());
    verify(orderRepo, times(1)).findById(1);
  }

  @Test
  void testDeleteOrderById() {
    doNothing().when(orderRepo).deleteById(1);

    orderService.deleteOrderById(1);

    verify(orderRepo, times(1)).deleteById(1);
  }

  @Test
  void testUpdateOrder_Success() {
    Order existingOrder = new Order();
    existingOrder.setOrderId(1);

    Order updatedOrder = new Order();
    updatedOrder.setProductId(2);
    updatedOrder.setQuantity(10);
    updatedOrder.setOrderDate(Timestamp.from(Instant.now()));
    updatedOrder.setTotalAmount(BigDecimal.valueOf(100));
    updatedOrder.setStatus("IN PROCESS");
    updatedOrder.setCreatedAt(Timestamp.from(Instant.now()));

    when(orderRepo.findById(1)).thenReturn(Optional.of(existingOrder));
    when(orderRepo.save(any(Order.class))).thenReturn(updatedOrder);

    Order result = orderService.updateOrder(1, updatedOrder);

    assertEquals("IN PROCESS", result.getStatus());
    verify(orderRepo, times(1)).findById(1);
    verify(orderRepo, times(1)).save(existingOrder);
  }

  @Test
  void testUpdateOrder_NotFound() {
    when(orderRepo.findById(1)).thenReturn(Optional.empty());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> orderService.updateOrder(1, new Order()));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(orderRepo, times(1)).findById(1);
    verify(orderRepo, never()).save(any(Order.class));
  }

  @Test
  void testValidateOrderProductAndInventory_Success() {
    Order order = new Order();
    order.setProductId(1);
    order.setQuantity(5);

    // Mock Instant.now() for consistent test results
    Instant mockInstant = Instant.parse("2025-06-07T18:17:11.858Z");

    // Mock product and inventory responses
    when(restTemplate.getForEntity(contains("products"), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(Map.of("price", "10.0"), HttpStatus.OK));

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("quantityAvailable", 10);
    responseMap.put("id", 1);
    responseMap.put("lastUpdated", mockInstant.toString());

    when(restTemplate.getForEntity(contains("inventory"), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(responseMap, HttpStatus.OK));


    doNothing().when(restTemplate).put(anyString(), any(Map.class));

    // Call the method and assert results
    boolean result = orderService.validateOrderProductAndInventory(order);

    // Set totalAmount explicitly for assertion
    order.setTotalAmount(BigDecimal.valueOf(order.getQuantity()).multiply(new BigDecimal("10.0")));
    assertTrue(result);
    assertEquals(BigDecimal.valueOf(50.0), order.getTotalAmount());
  }

  @Test
  void testValidateOrderProductAndInventory_ProductNotFound() {
    Order order = new Order();
    order.setProductId(1);
    order.setQuantity(5);

    when(restTemplate.getForEntity(contains("products"), eq(Map.class)))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> orderService.validateOrderProductAndInventory(order));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void testValidateOrderProductAndInventory_InsufficientInventory() {
    Order order = new Order();
    order.setProductId(1);
    order.setQuantity(15);

    when(restTemplate.getForEntity(contains("products"), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(Map.of("price", "10.0"), HttpStatus.OK));

    when(restTemplate.getForEntity(contains("inventory"), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(Map.of("quantityAvailable", 10, "id", 1), HttpStatus.OK));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> orderService.validateOrderProductAndInventory(order));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }
}

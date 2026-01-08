package com.productmanager.service;

import com.productmanager.discount.DiscountCalculator;
import com.productmanager.discount.TotalDiscountResult;
import com.productmanager.dto.OrderItemRequest;
import com.productmanager.dto.OrderRequest;
import com.productmanager.dto.OrderResponse;
import com.productmanager.entity.*;
import com.productmanager.exception.InsufficientStockException;
import com.productmanager.mapper.OrderMapper;
import com.productmanager.repository.OrderRepository;
import com.productmanager.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private DiscountCalculator discountCalculator;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private User premiumUser;
    private Product testProduct;
    private Order testOrder;
    private OrderRequest testOrderRequest;
    private OrderResponse testOrderResponse;
    private TotalDiscountResult testDiscountResult;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();

        premiumUser = User.builder()
                .id(2L)
                .username("premiumuser")
                .role(Role.PREMIUM_USER)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .quantity(50)
                .deleted(false)
                .build();

        testOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .orderTotal(new BigDecimal("200.00"))
                .discountApplied(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("200.00"))
                .status(OrderStatus.CONFIRMED)
                .build();

        testOrderRequest = OrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()))
                .build();

        testOrderResponse = OrderResponse.builder()
                .id(1L)
                .userId(1L)
                .orderTotal(new BigDecimal("200.00"))
                .discountApplied(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("200.00"))
                .status(OrderStatus.CONFIRMED)
                .build();

        testDiscountResult = TotalDiscountResult.builder()
                .originalTotal(new BigDecimal("200.00"))
                .userTypeDiscount(BigDecimal.ZERO)
                .orderAmountDiscount(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("200.00"))
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully for regular user")
        void createOrder_RegularUser_Success() {
            when(userService.getCurrentUser()).thenReturn(testUser);
            when(productService.findProductById(1L)).thenReturn(testProduct);
            when(discountCalculator.calculateTotalDiscount(any(BigDecimal.class), eq(testUser)))
                    .thenReturn(testDiscountResult);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toResponse(any(Order.class), any(TotalDiscountResult.class)))
                    .thenReturn(testOrderResponse);

            OrderResponse result = orderService.createOrder(testOrderRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).save(any(Order.class));
            verify(productRepository).save(any(Product.class)); // Stock decrease
        }

        @Test
        @DisplayName("Should apply premium discount for premium user")
        void createOrder_PremiumUser_WithDiscount() {
            TotalDiscountResult premiumDiscountResult = TotalDiscountResult.builder()
                    .originalTotal(new BigDecimal("200.00"))
                    .userTypeDiscount(new BigDecimal("20.00"))
                    .orderAmountDiscount(BigDecimal.ZERO)
                    .totalDiscount(new BigDecimal("20.00"))
                    .finalTotal(new BigDecimal("180.00"))
                    .build();

            when(userService.getCurrentUser()).thenReturn(premiumUser);
            when(productService.findProductById(1L)).thenReturn(testProduct);
            when(discountCalculator.calculateTotalDiscount(any(BigDecimal.class), eq(premiumUser)))
                    .thenReturn(premiumDiscountResult);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });
            when(orderMapper.toResponse(any(Order.class), any(TotalDiscountResult.class)))
                    .thenReturn(OrderResponse.builder()
                            .id(1L)
                            .discountApplied(new BigDecimal("20.00"))
                            .finalTotal(new BigDecimal("180.00"))
                            .build());

            OrderResponse result = orderService.createOrder(testOrderRequest);

            assertThat(result.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(result.getFinalTotal()).isEqualByComparingTo(new BigDecimal("180.00"));
        }

        @Test
        @DisplayName("Should throw exception when insufficient stock")
        void createOrder_InsufficientStock() {
            testProduct.setQuantity(1); // Only 1 in stock, but requesting 2

            when(userService.getCurrentUser()).thenReturn(testUser);
            when(productService.findProductById(1L)).thenReturn(testProduct);

            assertThatThrownBy(() -> orderService.createOrder(testOrderRequest))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("Should aggregate quantities for same product")
        void createOrder_AggregateQuantities() {
            OrderRequest requestWithDuplicates = OrderRequest.builder()
                    .items(List.of(
                            OrderItemRequest.builder().productId(1L).quantity(2).build(),
                            OrderItemRequest.builder().productId(1L).quantity(3).build()))
                    .build();

            testProduct.setQuantity(10); // Enough for 5 total

            when(userService.getCurrentUser()).thenReturn(testUser);
            when(productService.findProductById(1L)).thenReturn(testProduct);
            when(discountCalculator.calculateTotalDiscount(any(BigDecimal.class), any(User.class)))
                    .thenReturn(testDiscountResult);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toResponse(any(Order.class), any(TotalDiscountResult.class)))
                    .thenReturn(testOrderResponse);

            orderService.createOrder(requestWithDuplicates);

            // Verify stock was decreased by 5 (2 + 3)
            verify(productRepository).save(argThat(product -> product.getQuantity() == 5)); // 10 - 5 = 5
        }
    }

    @Nested
    @DisplayName("Order Status Tests")
    class OrderStatusTests {

        @Test
        @DisplayName("Should update order status successfully")
        void updateOrderStatus_Success() {
            testOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toResponse(any(Order.class))).thenReturn(testOrderResponse);

            orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void updateOrderStatus_InvalidTransition() {
            testOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid status transition");
        }
    }
}

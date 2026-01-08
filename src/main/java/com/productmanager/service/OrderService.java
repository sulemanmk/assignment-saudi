package com.productmanager.service;

import com.productmanager.discount.DiscountCalculator;
import com.productmanager.discount.TotalDiscountResult;
import com.productmanager.dto.*;
import com.productmanager.entity.*;
import com.productmanager.exception.InsufficientStockException;
import com.productmanager.exception.ResourceNotFoundException;
import com.productmanager.exception.UnauthorizedAccessException;
import com.productmanager.mapper.OrderMapper;
import com.productmanager.repository.OrderRepository;
import com.productmanager.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final UserService userService;
    private final DiscountCalculator discountCalculator;
    private final OrderMapper orderMapper;

    public OrderResponse createOrder(OrderRequest request) {
        User currentUser = userService.getCurrentUser();
        log.info("Creating order for user: {} with {} items", currentUser.getUsername(), request.getItems().size());

        // Validate and prepare order items
        Map<Long, Integer> productQuantities = aggregateQuantities(request.getItems());
        List<Product> products = validateAndFetchProducts(productQuantities);

        // Create order
        Order order = Order.builder()
                .user(currentUser)
                .status(OrderStatus.PENDING)
                .build();

        // Create order items and calculate totals
        BigDecimal orderTotal = BigDecimal.ZERO;
        for (Product product : products) {
            int quantity = productQuantities.get(product.getId());

            OrderItem orderItem = OrderItem.create(product, quantity);
            order.addItem(orderItem);

            orderTotal = orderTotal.add(orderItem.getTotalPrice());
        }

        // Calculate discount using the Strategy pattern
        TotalDiscountResult discountResult = discountCalculator.calculateTotalDiscount(orderTotal, currentUser);

        // Set order totals
        order.setOrderTotal(orderTotal);
        order.setDiscountApplied(discountResult.getTotalDiscount());
        order.setFinalTotal(discountResult.getFinalTotal());

        // Decrease product stock
        decreaseProductStock(products, productQuantities);

        // Confirm order
        order.setStatus(OrderStatus.CONFIRMED);

        // Save order
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully. Order ID: {}, Total: {}, Discount: {}, Final: {}",
                savedOrder.getId(), orderTotal, discountResult.getTotalDiscount(), discountResult.getFinalTotal());

        return orderMapper.toResponse(savedOrder, discountResult);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        User currentUser = userService.getCurrentUser();
        log.debug("Fetching order {} for user {}", id, currentUser.getUsername());

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // Check if user has access to this order
        if (!currentUser.isAdmin() && !order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You don't have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getCurrentUserOrders(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        log.debug("Fetching orders for user: {}", currentUser.getUsername());

        Page<Order> orderPage = orderRepository.findByUserId(currentUser.getId(), pageable);
        return toPageResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders (admin)");
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return toPageResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("Fetching orders for user id: {}", userId);

        // Verify user exists
        userService.findUserById(userId);

        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        return toPageResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Fetching orders with status: {}", status);
        Page<Order> orderPage = orderRepository.findByStatus(status, pageable);
        return toPageResponse(orderPage);
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order status updated successfully");
        return orderMapper.toResponse(updatedOrder);
    }

    public OrderResponse cancelOrder(Long id) {
        User currentUser = userService.getCurrentUser();
        log.info("Cancelling order {} by user {}", id, currentUser.getUsername());

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // Check if user has access to cancel this order
        if (!currentUser.isAdmin() && !order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You don't have permission to cancel this order");
        }

        // Only pending or confirmed orders can be cancelled
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Order cannot be cancelled in status: " + order.getStatus());
        }

        // Restore product stock
        restoreProductStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        log.info("Order cancelled successfully");
        return orderMapper.toResponse(cancelledOrder);
    }

    /**
     * Preview discount calculation without creating an order
     */
    @Transactional(readOnly = true)
    public TotalDiscountResult previewDiscount(OrderRequest request) {
        User currentUser = userService.getCurrentUser();

        Map<Long, Integer> productQuantities = aggregateQuantities(request.getItems());
        List<Product> products = productService.findProductsByIds(
                productQuantities.keySet().stream().toList());

        BigDecimal orderTotal = products.stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(productQuantities.get(product.getId()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return discountCalculator.calculateTotalDiscount(orderTotal, currentUser);
    }

    // Helper methods

    private Map<Long, Integer> aggregateQuantities(List<OrderItemRequest> items) {
        Map<Long, Integer> quantities = new HashMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private List<Product> validateAndFetchProducts(Map<Long, Integer> productQuantities) {
        List<Product> products = productQuantities.keySet().stream()
                .map(productService::findProductById)
                .toList();

        // Validate stock for all products
        for (Product product : products) {
            int requestedQuantity = productQuantities.get(product.getId());
            if (!product.hasStock(requestedQuantity)) {
                throw new InsufficientStockException(
                        product.getId(),
                        product.getName(),
                        requestedQuantity,
                        product.getQuantity());
            }
        }

        return products;
    }

    private void decreaseProductStock(List<Product> products, Map<Long, Integer> quantities) {
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            product.decreaseStock(quantity);
            productRepository.save(product);
            log.debug("Decreased stock for product {}: {} -> {}",
                    product.getId(), product.getQuantity() + quantity, product.getQuantity());
        }
    }

    private void restoreProductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
            log.debug("Restored stock for product {}: {} -> {}",
                    product.getId(), product.getQuantity() - item.getQuantity(), product.getQuantity());
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid transitions
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    private PageResponse<OrderResponse> toPageResponse(Page<Order> orderPage) {
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();
    }
}

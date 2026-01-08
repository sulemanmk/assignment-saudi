package com.productmanager.mapper;

import com.productmanager.discount.TotalDiscountResult;
import com.productmanager.dto.OrderItemResponse;
import com.productmanager.dto.OrderResponse;
import com.productmanager.entity.Order;
import com.productmanager.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .orderTotal(order.getOrderTotal())
                .discountApplied(order.getDiscountApplied())
                .finalTotal(order.getFinalTotal())
                .status(order.getStatus())
                .items(toOrderItemResponses(order.getItems()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderResponse toResponse(Order order, TotalDiscountResult discountResult) {
        OrderResponse response = toResponse(order);

        if (discountResult != null) {
            response.setDiscountBreakdown(OrderResponse.DiscountBreakdown.builder()
                    .userTypeDiscount(discountResult.getUserTypeDiscount())
                    .orderAmountDiscount(discountResult.getOrderAmountDiscount())
                    .totalDiscount(discountResult.getTotalDiscount())
                    .userTypeDiscountReason(discountResult.getUserTypeDiscountReason())
                    .orderAmountDiscountReason(discountResult.getOrderAmountDiscountReason())
                    .build());
        }

        return response;
    }

    public List<OrderItemResponse> toOrderItemResponses(List<OrderItem> items) {
        return items.stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountApplied(item.getDiscountApplied())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}

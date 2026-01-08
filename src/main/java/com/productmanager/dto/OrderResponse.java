package com.productmanager.dto;

import com.productmanager.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    private String username;
    private BigDecimal orderTotal;
    private BigDecimal discountApplied;
    private BigDecimal finalTotal;
    private OrderStatus status;
    private List<OrderItemResponse> items;
    private DiscountBreakdown discountBreakdown;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountBreakdown {
        private BigDecimal userTypeDiscount;
        private BigDecimal orderAmountDiscount;
        private BigDecimal totalDiscount;
        private String userTypeDiscountReason;
        private String orderAmountDiscountReason;
    }
}

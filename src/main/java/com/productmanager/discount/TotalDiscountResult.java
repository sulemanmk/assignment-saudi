package com.productmanager.discount;

import com.productmanager.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregated result of all discount calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalDiscountResult {

    private BigDecimal originalTotal;
    private BigDecimal userTypeDiscount;
    private BigDecimal orderAmountDiscount;
    private BigDecimal totalDiscount;
    private BigDecimal finalTotal;
    private String userTypeDiscountReason;
    private String orderAmountDiscountReason;

    public static TotalDiscountResult noDiscount(BigDecimal originalTotal) {
        return TotalDiscountResult.builder()
                .originalTotal(originalTotal)
                .userTypeDiscount(BigDecimal.ZERO)
                .orderAmountDiscount(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .finalTotal(originalTotal)
                .userTypeDiscountReason("No user type discount")
                .orderAmountDiscountReason("Order total under threshold")
                .build();
    }
}

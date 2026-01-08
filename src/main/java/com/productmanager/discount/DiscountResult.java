package com.productmanager.discount;

import com.productmanager.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the result of a discount calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResult {

    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private String reason;
    private DiscountType type;

    public enum DiscountType {
        USER_TYPE,
        ORDER_AMOUNT,
        PROMOTIONAL,
        NONE
    }

    public static DiscountResult none() {
        return DiscountResult.builder()
                .discountAmount(BigDecimal.ZERO)
                .discountPercentage(BigDecimal.ZERO)
                .reason("No discount applicable")
                .type(DiscountType.NONE)
                .build();
    }

    public static DiscountResult userTypeDiscount(BigDecimal amount, BigDecimal percentage, String userType) {
        return DiscountResult.builder()
                .discountAmount(amount)
                .discountPercentage(percentage)
                .reason(String.format("%s discount: %.0f%% off", userType, percentage))
                .type(DiscountType.USER_TYPE)
                .build();
    }

    public static DiscountResult orderAmountDiscount(BigDecimal amount, BigDecimal percentage, BigDecimal threshold) {
        return DiscountResult.builder()
                .discountAmount(amount)
                .discountPercentage(percentage)
                .reason(String.format("Order amount over $%.2f: %.0f%% extra discount", threshold, percentage))
                .type(DiscountType.ORDER_AMOUNT)
                .build();
    }
}

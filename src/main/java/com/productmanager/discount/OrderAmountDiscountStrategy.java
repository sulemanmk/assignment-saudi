package com.productmanager.discount;

import com.productmanager.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Discount strategy for orders exceeding a certain amount.
 * Orders over $500 receive an extra 5% discount for any user type.
 */
@Component
@Slf4j
public class OrderAmountDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal ORDER_THRESHOLD = new BigDecimal("500");
    private static final BigDecimal AMOUNT_DISCOUNT_PERCENTAGE = new BigDecimal("5");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public DiscountResult calculateDiscount(BigDecimal orderTotal, User user) {
        if (!isApplicable(orderTotal, user)) {
            return DiscountResult.none();
        }

        BigDecimal discountAmount = orderTotal
                .multiply(AMOUNT_DISCOUNT_PERCENTAGE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        log.debug("Applying order amount discount: {}% on orders over ${} = ${}",
                AMOUNT_DISCOUNT_PERCENTAGE, ORDER_THRESHOLD, discountAmount);

        return DiscountResult.orderAmountDiscount(discountAmount, AMOUNT_DISCOUNT_PERCENTAGE, ORDER_THRESHOLD);
    }

    @Override
    public boolean isApplicable(BigDecimal orderTotal, User user) {
        return orderTotal != null && orderTotal.compareTo(ORDER_THRESHOLD) > 0;
    }

    public BigDecimal getThreshold() {
        return ORDER_THRESHOLD;
    }

    public BigDecimal getDiscountPercentage() {
        return AMOUNT_DISCOUNT_PERCENTAGE;
    }
}

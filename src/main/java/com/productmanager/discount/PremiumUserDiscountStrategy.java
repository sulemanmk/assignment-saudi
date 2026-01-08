package com.productmanager.discount;

import com.productmanager.entity.Role;
import com.productmanager.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Discount strategy for premium users.
 * Premium users receive a 10% discount on their orders.
 */
@Component
@Slf4j
public class PremiumUserDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal PREMIUM_DISCOUNT_PERCENTAGE = new BigDecimal("10");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public DiscountResult calculateDiscount(BigDecimal orderTotal, User user) {
        if (!isApplicable(orderTotal, user)) {
            return DiscountResult.none();
        }

        BigDecimal discountAmount = orderTotal
                .multiply(PREMIUM_DISCOUNT_PERCENTAGE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        log.debug("Applying premium user discount: {}% = ${}", PREMIUM_DISCOUNT_PERCENTAGE, discountAmount);

        return DiscountResult.userTypeDiscount(discountAmount, PREMIUM_DISCOUNT_PERCENTAGE, "Premium User");
    }

    @Override
    public boolean isApplicable(BigDecimal orderTotal, User user) {
        return user != null && user.getRole() == Role.PREMIUM_USER;
    }
}

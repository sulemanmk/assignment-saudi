package com.productmanager.discount;

import com.productmanager.entity.User;

import java.math.BigDecimal;

/**
 * Strategy interface for discount calculation.
 * Implements the Strategy design pattern for dynamic discount computation.
 */
public interface DiscountStrategy {

    /**
     * Calculate the discount for a given order total and user.
     *
     * @param orderTotal the total amount of the order before discount
     * @param user       the user placing the order
     * @return the discount result containing amount and details
     */
    DiscountResult calculateDiscount(BigDecimal orderTotal, User user);

    /**
     * Check if this strategy is applicable for the given context.
     *
     * @param orderTotal the total amount of the order
     * @param user       the user placing the order
     * @return true if this strategy should be applied
     */
    boolean isApplicable(BigDecimal orderTotal, User user);
}

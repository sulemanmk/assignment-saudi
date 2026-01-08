package com.productmanager.discount;

import com.productmanager.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service that orchestrates discount calculation using multiple business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCalculator {

    private final PremiumUserDiscountStrategy premiumUserDiscountStrategy;
    private final OrderAmountDiscountStrategy orderAmountDiscountStrategy;

    /**
     * Calculate total discount for an order by combining multiple discount
     * strategies.
     *
     * @param orderTotal the total order amount before discounts
     * @param user       the user placing the order
     * @return the total discount result with breakdown
     */
    public TotalDiscountResult calculateTotalDiscount(BigDecimal orderTotal, User user) {
        log.info("Calculating discount for order total: {} for user: {} (role: {})",
                orderTotal, user.getUsername(), user.getRole());

        // Calculate user type discount (Premium users get 10%)
        DiscountResult userTypeResult = premiumUserDiscountStrategy.isApplicable(orderTotal, user)
                ? premiumUserDiscountStrategy.calculateDiscount(orderTotal, user)
                : DiscountResult.none();

        // Calculate order amount discount (Orders > $500 get extra 5%)
        DiscountResult orderAmountResult = orderAmountDiscountStrategy.isApplicable(orderTotal, user)
                ? orderAmountDiscountStrategy.calculateDiscount(orderTotal, user)
                : DiscountResult.none();

        // Sum up all discounts
        BigDecimal totalDiscount = userTypeResult.getDiscountAmount()
                .add(orderAmountResult.getDiscountAmount());

        BigDecimal finalTotal = orderTotal.subtract(totalDiscount);
        // Ensure final total is not negative
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        TotalDiscountResult result = TotalDiscountResult.builder()
                .originalTotal(orderTotal)
                .userTypeDiscount(userTypeResult.getDiscountAmount())
                .orderAmountDiscount(orderAmountResult.getDiscountAmount())
                .totalDiscount(totalDiscount)
                .finalTotal(finalTotal)
                .userTypeDiscountReason(userTypeResult.getReason())
                .orderAmountDiscountReason(orderAmountResult.getReason())
                .build();

        log.info(
                "Discount calculation complete. Original: {}, User discount: {}, Amount discount: {}, Total discount: {}, Final: {}",
                orderTotal, userTypeResult.getDiscountAmount(), orderAmountResult.getDiscountAmount(),
                totalDiscount, finalTotal);

        return result;
    }

    /**
     * Get a preview of potential discounts without creating an order.
     *
     * @param orderTotal the order total
     * @param user       the user
     * @return the discount preview
     */
    public TotalDiscountResult previewDiscount(BigDecimal orderTotal, User user) {
        return calculateTotalDiscount(orderTotal, user);
    }
}

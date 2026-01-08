package com.productmanager.discount;

import com.productmanager.entity.Role;
import com.productmanager.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Discount strategy for regular users.
 * Regular users receive no discount.
 */
@Component
@Slf4j
public class RegularUserDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountResult calculateDiscount(BigDecimal orderTotal, User user) {
        log.debug("Regular user - no user type discount applicable");
        return DiscountResult.none();
    }

    @Override
    public boolean isApplicable(BigDecimal orderTotal, User user) {
        return user != null && user.getRole() == Role.USER;
    }
}

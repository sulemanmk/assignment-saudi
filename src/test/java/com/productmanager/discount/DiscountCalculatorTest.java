package com.productmanager.discount;

import com.productmanager.entity.Role;
import com.productmanager.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountCalculator Tests")
class DiscountCalculatorTest {

    @Spy
    private PremiumUserDiscountStrategy premiumUserDiscountStrategy;

    @Spy
    private OrderAmountDiscountStrategy orderAmountDiscountStrategy;

    @InjectMocks
    private DiscountCalculator discountCalculator;

    private User regularUser;
    private User premiumUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .username("regular")
                .role(Role.USER)
                .build();

        premiumUser = User.builder()
                .id(2L)
                .username("premium")
                .role(Role.PREMIUM_USER)
                .build();

        adminUser = User.builder()
                .id(3L)
                .username("admin")
                .role(Role.ADMIN)
                .build();
    }

    @Nested
    @DisplayName("Regular User Discount Tests")
    class RegularUserDiscountTests {

        @Test
        @DisplayName("Regular user gets no discount on orders under $500")
        void regularUserNoDiscount() {
            BigDecimal orderTotal = new BigDecimal("200.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, regularUser);

            assertThat(result.getUserTypeDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getFinalTotal()).isEqualByComparingTo(orderTotal);
        }

        @Test
        @DisplayName("Regular user gets 5% discount on orders over $500")
        void regularUserOrderAmountDiscount() {
            BigDecimal orderTotal = new BigDecimal("600.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, regularUser);

            assertThat(result.getUserTypeDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getFinalTotal()).isEqualByComparingTo(new BigDecimal("570.00"));
        }
    }

    @Nested
    @DisplayName("Premium User Discount Tests")
    class PremiumUserDiscountTests {

        @Test
        @DisplayName("Premium user gets 10% discount on orders under $500")
        void premiumUserBasicDiscount() {
            BigDecimal orderTotal = new BigDecimal("200.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, premiumUser);

            assertThat(result.getUserTypeDiscount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(result.getFinalTotal()).isEqualByComparingTo(new BigDecimal("180.00"));
        }

        @Test
        @DisplayName("Premium user gets 10% + 5% discount on orders over $500")
        void premiumUserCombinedDiscount() {
            BigDecimal orderTotal = new BigDecimal("600.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, premiumUser);

            // 10% premium discount: $60
            // 5% order amount discount: $30
            // Total: $90
            assertThat(result.getUserTypeDiscount()).isEqualByComparingTo(new BigDecimal("60.00"));
            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("90.00"));
            assertThat(result.getFinalTotal()).isEqualByComparingTo(new BigDecimal("510.00"));
        }

        @Test
        @DisplayName("Premium user gets 15% total discount on $1000 order")
        void premiumUserLargeOrder() {
            BigDecimal orderTotal = new BigDecimal("1000.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, premiumUser);

            // 10% premium discount: $100
            // 5% order amount discount: $50
            // Total: $150
            assertThat(result.getUserTypeDiscount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.getFinalTotal()).isEqualByComparingTo(new BigDecimal("850.00"));
        }
    }

    @Nested
    @DisplayName("Admin User Discount Tests")
    class AdminUserDiscountTests {

        @Test
        @DisplayName("Admin user gets only order amount discount (not premium)")
        void adminUserOrderAmountDiscount() {
            BigDecimal orderTotal = new BigDecimal("600.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, adminUser);

            // Admin is not PREMIUM_USER, so no user type discount
            assertThat(result.getUserTypeDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getFinalTotal()).isEqualByComparingTo(new BigDecimal("570.00"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Exact $500 order does not trigger amount discount")
        void exactThresholdNoDiscount() {
            BigDecimal orderTotal = new BigDecimal("500.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, regularUser);

            assertThat(result.getOrderAmountDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("$500.01 order triggers amount discount")
        void justAboveThresholdGetsDiscount() {
            BigDecimal orderTotal = new BigDecimal("500.01");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, regularUser);

            assertThat(result.getOrderAmountDiscount()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Discount reasons are properly set")
        void discountReasonsAreSet() {
            BigDecimal orderTotal = new BigDecimal("600.00");

            TotalDiscountResult result = discountCalculator.calculateTotalDiscount(orderTotal, premiumUser);

            assertThat(result.getUserTypeDiscountReason()).contains("Premium User");
            assertThat(result.getOrderAmountDiscountReason()).contains("500");
        }
    }
}

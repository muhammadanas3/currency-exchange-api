package com.currency.service;

import com.currency.dto.CalculationRequest;
import com.currency.model.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class DiscountService {

    private static final BigDecimal EMPLOYEE_DISCOUNT = new BigDecimal("0.30");
    private static final BigDecimal AFFILIATE_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal LONG_TERM_CUSTOMER_DISCOUNT = new BigDecimal("0.05");
    private static final BigDecimal HUNDRED_DOLLAR_DISCOUNT = new BigDecimal("5.00");
    private static final BigDecimal HUNDRED_DOLLAR_THRESHOLD = new BigDecimal("100.00");

    public BigDecimal calculateDiscount(CalculationRequest request, BigDecimal totalAmount) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        BigDecimal maxDiscount = BigDecimal.ZERO;

        // Check if any items are groceries
        boolean hasNonGroceries = request.getItems().stream()
                .anyMatch(item -> item.getCategory() != CalculationRequest.ItemCategory.GROCERIES);

        if (hasNonGroceries) {
            // Apply role-based discounts
            switch (currentUser.getRole()) {
                case EMPLOYEE:
                    maxDiscount = totalAmount.multiply(EMPLOYEE_DISCOUNT);
                    break;
                case CASHIER: // Assuming CASHIER is equivalent to AFFILIATE
                    maxDiscount = totalAmount.multiply(AFFILIATE_DISCOUNT);
                    break;
                case CUSTOMER:
                    // Check if customer is long-term (more than 2 years)
                    if (isLongTermCustomer(currentUser)) {
                        maxDiscount = totalAmount.multiply(LONG_TERM_CUSTOMER_DISCOUNT);
                    }
                    break;
            }
        }

        // Apply $5 discount for every $100
        BigDecimal hundredDollarDiscount = totalAmount.divide(HUNDRED_DOLLAR_THRESHOLD, 0, RoundingMode.DOWN)
                .multiply(HUNDRED_DOLLAR_DISCOUNT);

        return maxDiscount.max(hundredDollarDiscount);
    }

    private boolean isLongTermCustomer(User user) {
        // Assuming user creation date is stored in the user entity
        // This is a placeholder - you'll need to add this field to your User entity
        LocalDateTime userCreationDate = user.getCreatedAt(); // You'll need to add this field
        return ChronoUnit.YEARS.between(userCreationDate, LocalDateTime.now()) >= 2;
    }
} 
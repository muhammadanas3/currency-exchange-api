package com.currency.controller;

import com.currency.dto.CalculationRequest;
import com.currency.dto.CalculationResponse;
import com.currency.service.CurrencyExchangeService;
import com.currency.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/calculate")
public class CalculationController {

    private final CurrencyExchangeService currencyExchangeService;
    private final DiscountService discountService;

    @Autowired
    public CalculationController(CurrencyExchangeService currencyExchangeService,
                               DiscountService discountService) {
        this.currencyExchangeService = currencyExchangeService;
        this.discountService = discountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CASHIER', 'CUSTOMER')")
    public ResponseEntity<CalculationResponse> calculate(@RequestBody CalculationRequest request) {
        // Calculate total amount in original currency
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate discount
        BigDecimal discountAmount = discountService.calculateDiscount(request, totalAmount);
        BigDecimal amountAfterDiscount = totalAmount.subtract(discountAmount);

        // Convert to target currency
        double exchangeRate = currencyExchangeService.getExchangeRate(
                request.getOriginalCurrency(),
                request.getTargetCurrency()
        );
        BigDecimal convertedAmount = amountAfterDiscount.multiply(BigDecimal.valueOf(exchangeRate));

        // Determine discount type
        String discountType = determineDiscountType(request, totalAmount, discountAmount);

        return ResponseEntity.ok(new CalculationResponse(
                totalAmount,
                request.getOriginalCurrency(),
                convertedAmount,
                request.getTargetCurrency(),
                discountAmount,
                discountType,
                convertedAmount
        ));
    }

    private String determineDiscountType(CalculationRequest request, BigDecimal totalAmount, BigDecimal discountAmount) {
        if (discountAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "No discount";
        }

        // Check if it's a $5 per $100 discount
        BigDecimal hundredDollarDiscount = totalAmount.divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                .multiply(new BigDecimal("5"));
        if (discountAmount.compareTo(hundredDollarDiscount) == 0) {
            return "$5 per $100 discount";
        }

        // Check role-based discounts
        boolean hasNonGroceries = request.getItems().stream()
                .anyMatch(item -> item.getCategory() != CalculationRequest.ItemCategory.GROCERIES);

        if (hasNonGroceries) {
            BigDecimal employeeDiscount = totalAmount.multiply(new BigDecimal("0.30"));
            BigDecimal affiliateDiscount = totalAmount.multiply(new BigDecimal("0.10"));
            BigDecimal longTermDiscount = totalAmount.multiply(new BigDecimal("0.05"));

            if (discountAmount.compareTo(employeeDiscount) == 0) {
                return "Employee discount (30%)";
            } else if (discountAmount.compareTo(affiliateDiscount) == 0) {
                return "Affiliate discount (10%)";
            } else if (discountAmount.compareTo(longTermDiscount) == 0) {
                return "Long-term customer discount (5%)";
            }
        }

        return "Unknown discount type";
    }
} 
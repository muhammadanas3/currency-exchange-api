package com.currency.controller;

import com.currency.dto.CalculationRequest;
import com.currency.dto.CalculationResponse;
import com.currency.service.CurrencyExchangeService;
import com.currency.service.DiscountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationControllerTest {

    @Mock
    private CurrencyExchangeService currencyExchangeService;

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private CalculationController calculationController;

    private CalculationRequest request;

    @BeforeEach
    void setUp() {
        // Setup test request
        request = new CalculationRequest();
        request.setOriginalCurrency("USD");
        request.setTargetCurrency("EUR");
        request.setItems(createTestItems());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void calculateWithEmployeeDiscount() {
        // Given
        BigDecimal totalAmount = new BigDecimal("1000.00");
        BigDecimal discountAmount = new BigDecimal("300.00"); // 30% employee discount
        double exchangeRate = 0.85; // USD to EUR

        when(discountService.calculateDiscount(any(), any())).thenReturn(discountAmount);
        when(currencyExchangeService.getExchangeRate("USD", "EUR")).thenReturn(exchangeRate);

        // When
        ResponseEntity<CalculationResponse> response = calculationController.calculate(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        CalculationResponse result = response.getBody();
        assertNotNull(result);
        assertEquals(totalAmount, result.getOriginalAmount());
        assertEquals("USD", result.getOriginalCurrency());
        assertEquals("EUR", result.getTargetCurrency());
        assertEquals(discountAmount, result.getDiscountAmount());
        assertEquals("Employee discount (30%)", result.getDiscountType());

        verify(discountService).calculateDiscount(any(), any());
        verify(currencyExchangeService).getExchangeRate("USD", "EUR");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void calculateWithNoDiscount() {
        // Given
        BigDecimal totalAmount = new BigDecimal("50.00");
        BigDecimal discountAmount = BigDecimal.ZERO;
        double exchangeRate = 0.85;

        when(discountService.calculateDiscount(any(), any())).thenReturn(discountAmount);
        when(currencyExchangeService.getExchangeRate("USD", "EUR")).thenReturn(exchangeRate);

        // When
        ResponseEntity<CalculationResponse> response = calculationController.calculate(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        CalculationResponse result = response.getBody();
        assertNotNull(result);
        assertEquals("No discount", result.getDiscountType());
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void calculateWithGroceriesOnly() {
        // Given
        List<CalculationRequest.Item> groceryItems = Arrays.asList(
                createItem("Milk", "10.00", CalculationRequest.ItemCategory.GROCERIES),
                createItem("Bread", "5.00", CalculationRequest.ItemCategory.GROCERIES)
        );
        request.setItems(groceryItems);

        BigDecimal totalAmount = new BigDecimal("15.00");
        BigDecimal discountAmount = BigDecimal.ZERO; // No discount on groceries
        double exchangeRate = 0.85;

        when(discountService.calculateDiscount(any(), any())).thenReturn(discountAmount);
        when(currencyExchangeService.getExchangeRate("USD", "EUR")).thenReturn(exchangeRate);

        // When
        ResponseEntity<CalculationResponse> response = calculationController.calculate(request);

        // Then
        assertNotNull(response);
        CalculationResponse result = response.getBody();
        assertNotNull(result);
        assertEquals("No discount", result.getDiscountType());
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    private List<CalculationRequest.Item> createTestItems() {
        return Arrays.asList(
                createItem("Laptop", "1000.00", CalculationRequest.ItemCategory.ELECTRONICS)
        );
    }

    private CalculationRequest.Item createItem(String name, String price, CalculationRequest.ItemCategory category) {
        CalculationRequest.Item item = new CalculationRequest.Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setCategory(category);
        return item;
    }
} 
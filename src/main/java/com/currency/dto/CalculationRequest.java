package com.currency.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CalculationRequest {
    @NotEmpty(message = "Items list cannot be empty")
    private List<Item> items;

    @NotNull(message = "Original currency cannot be null")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String originalCurrency;

    @NotNull(message = "Target currency cannot be null")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String targetCurrency;

    @Data
    public static class Item {
        @NotBlank(message = "Item name cannot be blank")
        private String name;

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private BigDecimal price;

        @NotNull(message = "Category cannot be null")
        private ItemCategory category;
    }

    public enum ItemCategory {
        GROCERIES,
        ELECTRONICS,
        CLOTHING,
        OTHER
    }
} 
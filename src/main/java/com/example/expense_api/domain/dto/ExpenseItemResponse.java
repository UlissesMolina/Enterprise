package com.example.expense_api.domain.dto;

import com.example.expense_api.domain.enums.ExpenseCategory;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class ExpenseItemResponse {
    Long id;
    ExpenseCategory category;
    BigDecimal amount;
    String description;
    Instant createdAt;
}

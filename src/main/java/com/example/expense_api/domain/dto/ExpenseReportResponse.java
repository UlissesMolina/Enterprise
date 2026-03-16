package com.example.expense_api.domain.dto;

import com.example.expense_api.domain.enums.ReportStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ExpenseReportResponse {
    Long id;
    ReportStatus status;
    BigDecimal totalAmount;
    Instant createdAt;
    Instant updatedAt;
    Long userId;
    String userEmail;
    String userDepartment;
    List<ExpenseItemResponse> items;
    List<ApprovalResponse> approvals;
}

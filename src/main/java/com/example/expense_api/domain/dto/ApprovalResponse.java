package com.example.expense_api.domain.dto;

import com.example.expense_api.domain.enums.ApprovalDecision;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ApprovalResponse {
    Long id;
    Long managerId;
    String managerEmail;
    ApprovalDecision decision;
    Instant decidedAt;
}

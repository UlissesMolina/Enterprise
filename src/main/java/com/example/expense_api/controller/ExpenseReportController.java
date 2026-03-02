package com.example.expense_api.controller;

import com.example.expense_api.domain.dto.ExpenseReportRequest;
import com.example.expense_api.domain.entity.ExpenseReport;
import com.example.expense_api.service.ExpenseReportService;
import com.example.expense_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Expense Reports")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseReportController {

    private final ExpenseReportService reportService;

    @PostMapping
    @Operation(summary = "Submit an expense report")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseReport> submitReport(@Valid @RequestBody ExpenseReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.submitReport(userId, request));
    }

    @PostMapping("/{reportId}/approve")
    @Operation(summary = "Approve a report (Manager/Admin only)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> approveReport(@PathVariable Long reportId) {
        Long managerId = SecurityUtils.getCurrentUserId();
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        reportService.approveReport(reportId, managerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{reportId}/reject")
    @Operation(summary = "Reject a report (Manager/Admin only)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> rejectReport(@PathVariable Long reportId) {
        Long managerId = SecurityUtils.getCurrentUserId();
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        reportService.rejectReport(reportId, managerId);
        return ResponseEntity.ok().build();
    }
}

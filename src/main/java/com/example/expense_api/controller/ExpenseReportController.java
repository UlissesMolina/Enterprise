package com.example.expense_api.controller;

import com.example.expense_api.domain.dto.ExpenseReportRequest;
import com.example.expense_api.domain.dto.ExpenseReportResponse;
import com.example.expense_api.domain.entity.ExpenseReport;
import com.example.expense_api.domain.enums.ReportStatus;
import com.example.expense_api.security.CustomUserDetails;
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

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Expense Reports")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseReportController {

    private final ExpenseReportService reportService;

    @GetMapping
    @Operation(summary = "Get expense reports (employees see own, managers/admins see all)")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ExpenseReportResponse>> getReports() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reportService.getReports(currentUser.getUserId(), currentUser.getRole()));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get expense reports by status (Manager/Admin only)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ExpenseReportResponse>> getReportsByStatus(@PathVariable ReportStatus status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get a specific expense report by ID")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseReportResponse> getReportById(@PathVariable Long reportId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reportService.getReportById(reportId, currentUser.getUserId(), currentUser.getRole()));
    }

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

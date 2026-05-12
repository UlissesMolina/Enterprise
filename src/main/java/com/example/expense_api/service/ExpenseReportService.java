package com.example.expense_api.service;

import com.example.expense_api.domain.dto.ApprovalResponse;
import com.example.expense_api.domain.dto.ExpenseItemRequest;
import com.example.expense_api.domain.dto.ExpenseItemResponse;
import com.example.expense_api.domain.dto.ExpenseReportRequest;
import com.example.expense_api.domain.dto.ExpenseReportResponse;
import com.example.expense_api.domain.entity.*;
import com.example.expense_api.domain.enums.ApprovalDecision;
import com.example.expense_api.domain.enums.ReportStatus;
import com.example.expense_api.domain.enums.RoleName;
import com.example.expense_api.exception.ReportAccessDeniedException;
import com.example.expense_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic for expense reports.
 * Multi-step transactional operations = backend engineering.
 */
@Service
@RequiredArgsConstructor
public class ExpenseReportService {

    private final ExpenseReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public ExpenseReportResponse submitReport(Long userId, ExpenseReportRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BigDecimal total = calculateTotal(request.getItems());

        ExpenseReport report = ExpenseReport.builder()
                .user(user)
                .status(ReportStatus.SUBMITTED)
                .totalAmount(total)
                .build();

        for (ExpenseItemRequest itemReq : request.getItems()) {
            ExpenseItem item = ExpenseItem.builder()
                    .report(report)
                    .category(itemReq.getCategory())
                    .amount(itemReq.getAmount())
                    .description(itemReq.getDescription())
                    .build();
            report.getExpenseItems().add(item);
        }

        report = reportRepository.save(report);

        auditLog(user, "SUBMIT_REPORT", "Report #" + report.getId() + " submitted");

        return toReportResponse(report);
    }

    @Transactional
    public void approveReport(Long reportId, Long managerId) {
        // 1. Fetch report
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        // 2. Validate role
        if (manager.getRole().getName() != RoleName.MANAGER && manager.getRole().getName() != RoleName.ADMIN) {
            throw new IllegalArgumentException("Only managers or admins can approve reports");
        }

        // 3. Ensure not self-approval
        if (report.getUser().getId().equals(managerId)) {
            throw new IllegalArgumentException("Cannot approve your own report");
        }

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new IllegalArgumentException("Report must be SUBMITTED to approve. Current status: " + report.getStatus());
        }

        // 4. Update status
        report.setStatus(ReportStatus.APPROVED);

        // 5. Create approval record
        Approval approval = Approval.builder()
                .report(report)
                .manager(manager)
                .decision(ApprovalDecision.APPROVED)
                .build();
        approvalRepository.save(approval);

        // 6. Create audit log
        auditLog(manager, "APPROVE_REPORT", "Report #" + reportId + " approved");

        reportRepository.save(report);
    }

    @Transactional
    public void rejectReport(Long reportId, Long managerId) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        if (manager.getRole().getName() != RoleName.MANAGER && manager.getRole().getName() != RoleName.ADMIN) {
            throw new IllegalArgumentException("Only managers or admins can reject reports");
        }

        if (report.getUser().getId().equals(managerId)) {
            throw new IllegalArgumentException("Cannot reject your own report");
        }

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new IllegalArgumentException("Report must be SUBMITTED to reject");
        }

        report.setStatus(ReportStatus.REJECTED);

        Approval approval = Approval.builder()
                .report(report)
                .manager(manager)
                .decision(ApprovalDecision.REJECTED)
                .build();
        approvalRepository.save(approval);

        auditLog(manager, "REJECT_REPORT", "Report #" + reportId + " rejected");

        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getReports(Long userId, RoleName role) {
        List<ExpenseReport> reports = (role == RoleName.EMPLOYEE)
                ? reportRepository.findByUserIdWithDetails(userId)
                : reportRepository.findAllWithDetails();
        return reports.stream().map(this::toReportResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseReportResponse getReportById(Long reportId, Long requestingUserId, RoleName role) {
        ExpenseReport report = reportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        if (role == RoleName.EMPLOYEE && !report.getUser().getId().equals(requestingUserId)) {
            throw new ReportAccessDeniedException("Access denied: you do not own this report");
        }
        return toReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<ExpenseReportResponse> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatusWithDetails(status)
                .stream().map(this::toReportResponse).collect(Collectors.toList());
    }

    private ExpenseItemResponse toItemResponse(ExpenseItem item) {
        return ExpenseItemResponse.builder()
                .id(item.getId())
                .category(item.getCategory())
                .amount(item.getAmount())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private ApprovalResponse toApprovalResponse(Approval approval) {
        return ApprovalResponse.builder()
                .id(approval.getId())
                .managerId(approval.getManager().getId())
                .managerEmail(approval.getManager().getEmail())
                .decision(approval.getDecision())
                .decidedAt(approval.getCreatedAt())
                .build();
    }

    private ExpenseReportResponse toReportResponse(ExpenseReport report) {
        String department = (report.getUser().getDepartment() != null)
                ? report.getUser().getDepartment().getName()
                : null;
        return ExpenseReportResponse.builder()
                .id(report.getId())
                .status(report.getStatus())
                .totalAmount(report.getTotalAmount())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .userId(report.getUser().getId())
                .userEmail(report.getUser().getEmail())
                .userDepartment(department)
                .items(report.getExpenseItems().stream().map(this::toItemResponse).collect(Collectors.toList()))
                .approvals(report.getApprovals().stream().map(this::toApprovalResponse).collect(Collectors.toList()))
                .build();
    }

    public BigDecimal calculateTotal(List<ExpenseItemRequest> items) {
        return items.stream()
                .map(ExpenseItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void auditLog(User performedBy, String action, String details) {
        AuditLog log = AuditLog.builder()
                .action(action + ": " + details)
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(log);
    }
}

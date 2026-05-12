package com.example.expense_api.service;

import com.example.expense_api.domain.dto.ExpenseItemRequest;
import com.example.expense_api.domain.dto.ExpenseReportRequest;
import com.example.expense_api.domain.dto.ExpenseReportResponse;
import com.example.expense_api.domain.entity.*;
import com.example.expense_api.domain.enums.ExpenseCategory;
import com.example.expense_api.domain.enums.ReportStatus;
import com.example.expense_api.domain.enums.RoleName;
import com.example.expense_api.exception.ReportAccessDeniedException;
import com.example.expense_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseReportServiceTest {

    @Mock private ExpenseReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ExpenseReportService reportService;

    private User employee;
    private User manager;
    private Role employeeRole;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        employeeRole = new Role();
        employeeRole.setId(1L);
        employeeRole.setName(RoleName.EMPLOYEE);

        managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.MANAGER);

        employee = User.builder().email("emp@test.com").role(employeeRole).passwordHash("x").build();
        employee.setId(1L);

        manager = User.builder().email("mgr@test.com").role(managerRole).passwordHash("x").build();
        manager.setId(2L);
    }

    @Test
    void submitReport_success() {
        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .items(List.of(
                        ExpenseItemRequest.builder()
                                .category(ExpenseCategory.TRAVEL)
                                .amount(new BigDecimal("100.00"))
                                .description("Flight")
                                .build(),
                        ExpenseItemRequest.builder()
                                .category(ExpenseCategory.MEALS)
                                .amount(new BigDecimal("50.00"))
                                .build()
                )).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reportRepository.save(any(ExpenseReport.class))).thenAnswer(inv -> {
            ExpenseReport r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        ExpenseReportResponse result = reportService.submitReport(1L, request);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(result.getItems()).hasSize(2);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void approveReport_success() {
        ExpenseReport report = ExpenseReport.builder()
                .user(employee).status(ReportStatus.SUBMITTED).totalAmount(BigDecimal.TEN).build();
        report.setId(10L);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        reportService.approveReport(10L, 2L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
        verify(approvalRepository).save(any(Approval.class));
        verify(reportRepository).save(report);
    }

    @Test
    void approveReport_selfApproval_throws() {
        ExpenseReport report = ExpenseReport.builder()
                .user(manager).status(ReportStatus.SUBMITTED).totalAmount(BigDecimal.TEN).build();
        report.setId(10L);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> reportService.approveReport(10L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot approve your own report");
    }

    @Test
    void rejectReport_success() {
        ExpenseReport report = ExpenseReport.builder()
                .user(employee).status(ReportStatus.SUBMITTED).totalAmount(BigDecimal.TEN).build();
        report.setId(10L);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        reportService.rejectReport(10L, 2L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        verify(approvalRepository).save(any(Approval.class));
    }

    @Test
    void approveReport_alreadyApproved_throws() {
        ExpenseReport report = ExpenseReport.builder()
                .user(employee).status(ReportStatus.APPROVED).totalAmount(BigDecimal.TEN).build();
        report.setId(10L);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> reportService.approveReport(10L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be SUBMITTED");
    }

    @Test
    void getReportById_employeeAccessDenied() {
        Department dept = Department.builder().name("Engineering").build();
        dept.setId(1L);
        employee.setDepartment(dept);

        ExpenseReport report = ExpenseReport.builder()
                .user(manager).status(ReportStatus.SUBMITTED).totalAmount(BigDecimal.TEN).build();
        report.setId(10L);
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());

        when(reportRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.getReportById(10L, 1L, RoleName.EMPLOYEE))
                .isInstanceOf(ReportAccessDeniedException.class);
    }

    @Test
    void getReports_employeeSeeOwn() {
        when(reportRepository.findByUserIdWithDetails(1L)).thenReturn(List.of());

        List<ExpenseReportResponse> result = reportService.getReports(1L, RoleName.EMPLOYEE);

        assertThat(result).isEmpty();
        verify(reportRepository).findByUserIdWithDetails(1L);
        verify(reportRepository, never()).findAllWithDetails();
    }

    @Test
    void getReports_managerSeesAll() {
        when(reportRepository.findAllWithDetails()).thenReturn(List.of());

        List<ExpenseReportResponse> result = reportService.getReports(2L, RoleName.MANAGER);

        assertThat(result).isEmpty();
        verify(reportRepository).findAllWithDetails();
        verify(reportRepository, never()).findByUserIdWithDetails(any());
    }
}

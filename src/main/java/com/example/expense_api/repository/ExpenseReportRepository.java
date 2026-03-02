package com.example.expense_api.repository;

import com.example.expense_api.domain.entity.ExpenseReport;
import com.example.expense_api.domain.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {

    List<ExpenseReport> findByUserId(Long userId);

    List<ExpenseReport> findByStatus(ReportStatus status);
}

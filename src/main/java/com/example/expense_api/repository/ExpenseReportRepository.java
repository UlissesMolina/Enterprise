package com.example.expense_api.repository;

import com.example.expense_api.domain.entity.ExpenseReport;
import com.example.expense_api.domain.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {

    List<ExpenseReport> findByUserId(Long userId);

    List<ExpenseReport> findByStatus(ReportStatus status);

    @Query("""
            SELECT DISTINCT r FROM ExpenseReport r
            LEFT JOIN FETCH r.expenseItems
            LEFT JOIN FETCH r.approvals a
            LEFT JOIN FETCH a.manager
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.department
            WHERE r.id = :id
            """)
    Optional<ExpenseReport> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT r FROM ExpenseReport r
            LEFT JOIN FETCH r.expenseItems
            LEFT JOIN FETCH r.approvals a
            LEFT JOIN FETCH a.manager
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.department
            WHERE u.id = :userId
            """)
    List<ExpenseReport> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT r FROM ExpenseReport r
            LEFT JOIN FETCH r.expenseItems
            LEFT JOIN FETCH r.approvals a
            LEFT JOIN FETCH a.manager
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.department
            WHERE r.status = :status
            """)
    List<ExpenseReport> findByStatusWithDetails(@Param("status") ReportStatus status);

    @Query("""
            SELECT DISTINCT r FROM ExpenseReport r
            LEFT JOIN FETCH r.expenseItems
            LEFT JOIN FETCH r.approvals a
            LEFT JOIN FETCH a.manager
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.department
            """)
    List<ExpenseReport> findAllWithDetails();
}

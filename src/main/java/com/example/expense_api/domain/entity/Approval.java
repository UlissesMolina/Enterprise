package com.example.expense_api.domain.entity;

import com.example.expense_api.domain.enums.ApprovalDecision;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approvals", indexes = {
    @Index(name = "idx_approval_report", columnList = "report_id"),
    @Index(name = "idx_approval_manager", columnList = "manager_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "createdAt", column = @Column(name = "decided_at", nullable = false, updatable = false))
public class Approval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ExpenseReport report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;
}

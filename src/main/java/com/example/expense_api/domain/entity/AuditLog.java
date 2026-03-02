package com.example.expense_api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_performed_by", columnList = "performed_by"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "createdAt", column = @Column(name = "timestamp", nullable = false, updatable = false))
public class AuditLog extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
}

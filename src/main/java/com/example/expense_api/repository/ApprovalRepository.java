package com.example.expense_api.repository;

import com.example.expense_api.domain.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
}

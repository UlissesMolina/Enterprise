package com.example.expense_api.repository;

import com.example.expense_api.domain.entity.Role;
import com.example.expense_api.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}

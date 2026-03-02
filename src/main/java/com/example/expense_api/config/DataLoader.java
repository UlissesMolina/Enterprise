package com.example.expense_api.config;

import com.example.expense_api.domain.entity.Department;
import com.example.expense_api.domain.entity.Role;
import com.example.expense_api.domain.enums.RoleName;
import com.example.expense_api.repository.DepartmentRepository;
import com.example.expense_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds roles and departments on startup.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    Role.builder().name(RoleName.EMPLOYEE).build(),
                    Role.builder().name(RoleName.MANAGER).build(),
                    Role.builder().name(RoleName.ADMIN).build()
            ));
        }

        if (departmentRepository.count() == 0) {
            departmentRepository.saveAll(List.of(
                    Department.builder().name("Engineering").monthlyBudget(new BigDecimal("50000")).build(),
                    Department.builder().name("Sales").monthlyBudget(new BigDecimal("30000")).build(),
                    Department.builder().name("Marketing").monthlyBudget(new BigDecimal("20000")).build()
            ));
        }
    }
}

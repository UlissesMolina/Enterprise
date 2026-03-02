package com.example.expense_api.service;

import com.example.expense_api.domain.dto.AuthResponse;
import com.example.expense_api.domain.dto.LoginRequest;
import com.example.expense_api.domain.dto.RegisterRequest;
import com.example.expense_api.domain.entity.Department;
import com.example.expense_api.domain.entity.Role;
import com.example.expense_api.domain.entity.User;
import com.example.expense_api.domain.enums.RoleName;
import com.example.expense_api.repository.DepartmentRepository;
import com.example.expense_api.repository.RoleRepository;
import com.example.expense_api.repository.UserRepository;
import com.example.expense_api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("EMPLOYEE role not found"));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(employeeRole)
                .department(department)
                .build();

        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String email = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("EMPLOYEE");

        String token = tokenProvider.generateToken(email);

        return AuthResponse.builder()
                .token(token)
                .email(email)
                .role(role)
                .build();
    }
}

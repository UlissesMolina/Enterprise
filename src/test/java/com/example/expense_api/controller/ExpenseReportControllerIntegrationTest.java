package com.example.expense_api.controller;

import com.example.expense_api.domain.dto.ExpenseItemRequest;
import com.example.expense_api.domain.dto.ExpenseReportRequest;
import com.example.expense_api.domain.dto.RegisterRequest;
import com.example.expense_api.domain.entity.Role;
import com.example.expense_api.domain.entity.User;
import com.example.expense_api.domain.enums.ExpenseCategory;
import com.example.expense_api.domain.enums.RoleName;
import com.example.expense_api.repository.RoleRepository;
import com.example.expense_api.repository.UserRepository;
import com.example.expense_api.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseReportControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private String employeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE).orElseThrow();
        Role managerRole = roleRepository.findByName(RoleName.MANAGER).orElseThrow();

        User employee = User.builder()
                .email("employee@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(employeeRole)
                .build();
        userRepository.save(employee);

        User manager = User.builder()
                .email("manager@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(managerRole)
                .build();
        userRepository.save(manager);

        employeeToken = tokenProvider.generateToken("employee@test.com");
        managerToken = tokenProvider.generateToken("manager@test.com");
    }

    @Test
    void submitReport_success() throws Exception {
        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .items(List.of(
                        ExpenseItemRequest.builder()
                                .category(ExpenseCategory.TRAVEL)
                                .amount(new BigDecimal("250.00"))
                                .description("Flight to NYC")
                                .build()
                )).build();

        mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.totalAmount").value(250.00));
    }

    @Test
    void submitReport_unauthenticated_returns401() throws Exception {
        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .items(List.of(
                        ExpenseItemRequest.builder()
                                .category(ExpenseCategory.TRAVEL)
                                .amount(new BigDecimal("100.00"))
                                .build()
                )).build();

        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReports_employeeSeesOwnOnly() throws Exception {
        // Submit as employee
        submitReportAs(employeeToken);

        mockMvc.perform(get("/reports")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void approveReport_managerSuccess() throws Exception {
        // Submit as employee
        String reportJson = submitReportAs(employeeToken);
        Long reportId = objectMapper.readTree(reportJson).get("id").asLong();

        // Approve as manager
        mockMvc.perform(post("/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        // Verify status changed
        mockMvc.perform(get("/reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectReport_managerSuccess() throws Exception {
        String reportJson = submitReportAs(employeeToken);
        Long reportId = objectMapper.readTree(reportJson).get("id").asLong();

        mockMvc.perform(post("/reports/" + reportId + "/reject")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void approveReport_employeeForbidden() throws Exception {
        String reportJson = submitReportAs(employeeToken);
        Long reportId = objectMapper.readTree(reportJson).get("id").asLong();

        mockMvc.perform(post("/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReportsByStatus_employeeForbidden() throws Exception {
        mockMvc.perform(get("/reports/status/SUBMITTED")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReportsByStatus_managerSuccess() throws Exception {
        submitReportAs(employeeToken);

        mockMvc.perform(get("/reports/status/SUBMITTED")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private String submitReportAs(String token) throws Exception {
        ExpenseReportRequest request = ExpenseReportRequest.builder()
                .items(List.of(
                        ExpenseItemRequest.builder()
                                .category(ExpenseCategory.TRAVEL)
                                .amount(new BigDecimal("100.00"))
                                .description("Test expense")
                                .build()
                )).build();

        MvcResult result = mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getContentAsString();
    }
}

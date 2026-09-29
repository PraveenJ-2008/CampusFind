package com.campusfind.controller;

import com.campusfind.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testStudentCannotAccessAdminDashboard() throws Exception {
        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCanAccessDashboardMetrics() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@campusfind.edu", "ADMIN", 1L, "Campus Admin");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLostReports").isNumber())
                .andExpect(jsonPath("$.data.totalFoundItems").isNumber())
                .andExpect(jsonPath("$.data.claimedFoundItems").isNumber())
                .andExpect(jsonPath("$.data.returnedFoundItems").isNumber());
    }

    @Test
    void testAdminCanListAllUsers() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@campusfind.edu", "ADMIN", 1L, "Campus Admin");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}

package com.campusfind.controller;

import com.campusfind.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGetCategoriesPublicly() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testCreateCategoryRequiresAdmin() throws Exception {
        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        String categoryJson = """
                {
                    "name": "Gym Gear",
                    "description": "Sports and gym equipment",
                    "active": true
                }
                """;

        // Student attempt should be 403 Forbidden
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isForbidden());

        // Admin attempt should be 201 Created
        String adminToken = jwtUtil.generateToken("admin@campusfind.edu", "ADMIN", 1L, "Campus Admin");
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Gym Gear"));
    }

    @Test
    void testCreateDuplicateCategoryReturnsConflict() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@campusfind.edu", "ADMIN", 1L, "Campus Admin");

        String duplicateCategoryJson = """
                {
                    "name": "Electronics",
                    "description": "Duplicate category test",
                    "active": true
                }
                """;

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateCategoryJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}

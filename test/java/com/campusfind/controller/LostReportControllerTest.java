package com.campusfind.controller;

import com.campusfind.entity.Category;
import com.campusfind.repository.CategoryRepository;
import com.campusfind.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LostReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testGetLostReportsPublicly() throws Exception {
        mockMvc.perform(get("/api/lost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testCreateLostReportWithoutAuthFails() throws Exception {
        String payload = """
                {
                    "title": "Lost Wallet",
                    "categoryId": 1,
                    "description": "Black leather wallet",
                    "location": "Canteen",
                    "lostDate": "2026-09-20"
                }
                """;

        mockMvc.perform(post("/api/lost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateLostReportWithInvalidDataReturnsBadRequest() throws Exception {
        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        // Missing required fields
        String invalidPayload = """
                {
                    "title": "",
                    "description": ""
                }
                """;

        mockMvc.perform(post("/api/lost")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void testCreateLostReportSuccess() throws Exception {
        Category category = categoryRepository.findAll().getFirst();
        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        String validPayload = String.format("""
                {
                    "title": "Lost Physics Textbook",
                    "categoryId": %d,
                    "description": "Engineering Physics 2nd edition with yellow bookmark",
                    "location": "Library Room 101",
                    "lostDate": "%s"
                }
                """, category.getId(), LocalDate.now().toString());

        mockMvc.perform(post("/api/lost")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Lost Physics Textbook"));
    }
}

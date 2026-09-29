package com.campusfind.controller;

import com.campusfind.entity.Category;
import com.campusfind.entity.Role;
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
class FoundItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testGetFoundItemsPublicly() throws Exception {
        mockMvc.perform(get("/api/found"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testStudentCannotCreateFoundItem() throws Exception {
        Category category = categoryRepository.findAll().getFirst();
        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        String payload = String.format("""
                {
                    "title": "Found Wristwatch",
                    "categoryId": %d,
                    "description": "Silver analog watch",
                    "location": "Gymnasium",
                    "foundDate": "%s"
                }
                """, category.getId(), LocalDate.now());

        mockMvc.perform(post("/api/found")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStaffCanCreateFoundItem() throws Exception {
        Category category = categoryRepository.findAll().getFirst();
        String staffToken = jwtUtil.generateToken("staff@campusfind.edu", "STAFF", 3L, "Campus Security");

        String payload = String.format("""
                {
                    "title": "Found Water Bottle",
                    "categoryId": %d,
                    "description": "Steel insulated Milton bottle",
                    "location": "Cafeteria",
                    "foundDate": "%s"
                }
                """, category.getId(), LocalDate.now());

        mockMvc.perform(post("/api/found")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FOUND"));
    }

    @Test
    void testDirectTransitionFromFoundToReturnedIsRejected() throws Exception {
        Category category = categoryRepository.findAll().getFirst();
        String staffToken = jwtUtil.generateToken("staff@campusfind.edu", "STAFF", 3L, "Campus Security");

        // 1. Create found item
        String createPayload = String.format("""
                {
                    "title": "Found Scientific Calculator",
                    "categoryId": %d,
                    "description": "Casio FX calculator",
                    "location": "Library Room 2",
                    "foundDate": "%s"
                }
                """, category.getId(), LocalDate.now());

        String createRes = mockMvc.perform(post("/api/found")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract ID
        com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(createRes);
        long itemId = jsonNode.path("data").path("id").asLong();

        // 2. Try transitioning directly to RETURNED
        String statusPayload = """
                {
                    "status": "RETURNED"
                }
                """;

        mockMvc.perform(patch("/api/found/" + itemId + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

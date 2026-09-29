package com.campusfind.controller;

import com.campusfind.dto.FoundItemRequest;
import com.campusfind.dto.LostReportRequest;
import com.campusfind.entity.Category;
import com.campusfind.repository.CategoryRepository;
import com.campusfind.security.JwtUtil;
import com.campusfind.service.FoundItemService;
import com.campusfind.service.LostReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LostReportService lostReportService;

    @Autowired
    private FoundItemService foundItemService;

    @Test
    void testGetMatchSuggestionsForLostReport() throws Exception {
        Category category = categoryRepository.findAll().getFirst();

        // Student creates lost report for blue water bottle
        var lost = lostReportService.createReport(
                new LostReportRequest("Blue Milton Bottle", category.getId(), "Steel bottle 1000ml", "Cafeteria", LocalDate.now()),
                "student@campusfind.edu"
        );

        // Staff creates found item for blue water bottle
        foundItemService.createFoundItem(
                new FoundItemRequest("Blue Milton Steel Bottle", category.getId(), "Found 1000ml bottle", "Cafeteria table", LocalDate.now()),
                "staff@campusfind.edu"
        );

        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        mockMvc.perform(get("/api/matches/" + lost.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].score").isNumber());
    }

    @Test
    void testConfirmMatchUpdatesStatuses() throws Exception {
        Category category = categoryRepository.findAll().getFirst();

        var lost = lostReportService.createReport(
                new LostReportRequest("Notebook Math", category.getId(), "Engineering math notes", "Room 303", LocalDate.now()),
                "student@campusfind.edu"
        );

        var found = foundItemService.createFoundItem(
                new FoundItemRequest("Math Engineering Notebook", category.getId(), "Spiral bound notebook", "Room 303", LocalDate.now()),
                "staff@campusfind.edu"
        );

        String studentToken = jwtUtil.generateToken("student@campusfind.edu", "STUDENT", 2L, "Demo Student");

        mockMvc.perform(post("/api/matches/" + lost.getId() + "/confirm")
                        .param("foundId", found.getId().toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

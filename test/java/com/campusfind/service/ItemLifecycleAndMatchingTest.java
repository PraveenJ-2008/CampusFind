package com.campusfind.service;

import com.campusfind.dto.*;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.ReportStatus;
import com.campusfind.entity.Role;
import com.campusfind.exception.BadRequestException;
import com.campusfind.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemLifecycleAndMatchingTest {

    @Autowired
    private LostReportService lostReportService;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AuthService authService;

    private Long categoryId;
    private String student1Email;
    private String student2Email;
    private String staffEmail;

    @BeforeEach
    void setUp() {
        student1Email = "student1_" + System.currentTimeMillis() + "@campusfind.edu";
        student2Email = "student2_" + System.currentTimeMillis() + "@campusfind.edu";
        staffEmail = "staff_" + System.currentTimeMillis() + "@campusfind.edu";

        authService.register(new RegisterRequest("Student One", student1Email, "Pass@123", Role.STUDENT));
        authService.register(new RegisterRequest("Student Two", student2Email, "Pass@123", Role.STUDENT));
        authService.register(new RegisterRequest("Staff Member", staffEmail, "Pass@123", Role.STAFF));

        List<CategoryDto> categories = categoryService.getActiveCategories();
        categoryId = categories.getFirst().getId();
    }

    @Test
    void testCreateLostReport() {
        LostReportRequest request = new LostReportRequest(
                "HP Scientific Calculator",
                categoryId,
                "Black HP Prime calculator left in Room 402",
                "Academic Block Room 402",
                LocalDate.now()
        );

        LostReportResponse response = lostReportService.createReport(request, student1Email);
        assertNotNull(response.getId());
        assertEquals("HP Scientific Calculator", response.getTitle());
        assertEquals(ReportStatus.OPEN, response.getStatus());
    }

    @Test
    void testStudentCannotCreateFoundItem() {
        FoundItemRequest request = new FoundItemRequest(
                "Found Watch",
                categoryId,
                "Silver wrist watch",
                "Sports Complex",
                LocalDate.now()
        );

        assertThrows(ForbiddenException.class, () -> foundItemService.createFoundItem(request, student1Email));
    }

    @Test
    void testStaffCanCreateFoundItem() {
        FoundItemRequest request = new FoundItemRequest(
                "Found Watch",
                categoryId,
                "Silver wrist watch",
                "Sports Complex",
                LocalDate.now()
        );

        FoundItemResponse response = foundItemService.createFoundItem(request, staffEmail);
        assertNotNull(response.getId());
        assertEquals(ItemStatus.FOUND, response.getStatus());
    }

    @Test
    void testInvalidStatusTransitionFoundToReturnedIsRejected() {
        FoundItemRequest request = new FoundItemRequest(
                "Titan Silver Watch",
                categoryId,
                "Titan silver analog watch found at playground",
                "Playground Stand",
                LocalDate.now()
        );
        FoundItemResponse item = foundItemService.createFoundItem(request, staffEmail);

        // Transition directly from FOUND to RETURNED must fail
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.RETURNED, staffEmail)
        );
        assertTrue(ex.getMessage().contains("to RETURNED. It must first be CLAIMED"));
    }

    @Test
    void testValidStatusTransitionFoundToClaimedToReturned() {
        FoundItemRequest request = new FoundItemRequest(
                "Fastrack Blue Watch",
                categoryId,
                "Blue silicone strap watch",
                "Gymnasium",
                LocalDate.now()
        );
        FoundItemResponse item = foundItemService.createFoundItem(request, staffEmail);

        // FOUND -> CLAIMED
        FoundItemResponse claimed = foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.CLAIMED, staffEmail);
        assertEquals(ItemStatus.CLAIMED, claimed.getStatus());

        // CLAIMED -> RETURNED
        FoundItemResponse returned = foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.RETURNED, staffEmail);
        assertEquals(ItemStatus.RETURNED, returned.getStatus());

        // Staff cannot reopen RETURNED item
        assertThrows(BadRequestException.class, () ->
                foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.FOUND, staffEmail)
        );

        // Admin can reopen RETURNED item
        FoundItemResponse reopened = foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.FOUND, "admin@campusfind.edu");
        assertEquals(ItemStatus.FOUND, reopened.getStatus());
    }

    @Test
    void testStatusTransitionClaimedBackToFound() {
        FoundItemRequest request = new FoundItemRequest(
                "Scientific Calculator Casio",
                categoryId,
                "Calculator found in Room 101",
                "Room 101",
                LocalDate.now()
        );
        FoundItemResponse item = foundItemService.createFoundItem(request, staffEmail);

        // FOUND -> CLAIMED
        foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.CLAIMED, staffEmail);

        // Claim unverified -> back to FOUND
        FoundItemResponse reopened = foundItemService.updateFoundItemStatus(item.getId(), ItemStatus.FOUND, staffEmail);
        assertEquals(ItemStatus.FOUND, reopened.getStatus());
    }

    @Test
    void testDeleteLostReportByOwnerAndForbiddenForOthers() {
        LostReportRequest request = new LostReportRequest(
                "Keys Bundle",
                categoryId,
                "Bike and dorm keys on blue lanyard",
                "Parking Lot",
                LocalDate.now()
        );
        LostReportResponse report = lostReportService.createReport(request, student1Email);

        // Student 2 cannot delete Student 1's report
        assertThrows(ForbiddenException.class, () ->
                lostReportService.deleteReport(report.getId(), student2Email)
        );

        // Student 1 can delete their own report
        assertDoesNotThrow(() -> lostReportService.deleteReport(report.getId(), student1Email));
    }

    @Test
    void testDeleteFoundItemByStaffAndForbiddenForStudent() {
        FoundItemRequest request = new FoundItemRequest(
                "Umbrella",
                categoryId,
                "Black folding umbrella",
                "Canteen",
                LocalDate.now()
        );
        FoundItemResponse item = foundItemService.createFoundItem(request, staffEmail);

        // Student cannot delete staff's found item
        assertThrows(ForbiddenException.class, () ->
                foundItemService.deleteFoundItem(item.getId(), student1Email)
        );

        // Staff member who created it can delete it
        assertDoesNotThrow(() -> foundItemService.deleteFoundItem(item.getId(), staffEmail));
    }

    @Test
    void testOtherStudentCannotModifyReport() {
        LostReportRequest request = new LostReportRequest(
                "Sony Headphones",
                categoryId,
                "Wireless black Sony headphones",
                "Seminar Hall",
                LocalDate.now()
        );
        LostReportResponse report = lostReportService.createReport(request, student1Email);

        LostReportRequest updateReq = new LostReportRequest(
                "Hacked Sony Headphones",
                categoryId,
                "Hacked description",
                "Seminar Hall",
                LocalDate.now()
        );

        // Student 2 tries to update Student 1's report
        assertThrows(ForbiddenException.class, () ->
                lostReportService.updateReport(report.getId(), updateReq, student2Email)
        );
    }

    @Test
    void testMatchingEngineGeneratesHighSimilarityScore() {
        LostReportRequest lostReq = new LostReportRequest(
                "Casio FX-991CW Advanced Calculator",
                categoryId,
                "Lost my white Casio FX-991CW calculator in Electrical Lab",
                "Electrical Lab 2",
                LocalDate.now()
        );
        LostReportResponse lostReport = lostReportService.createReport(lostReq, student1Email);

        FoundItemRequest foundReq = new FoundItemRequest(
                "White Casio FX-991CW Calculator",
                categoryId,
                "Found white colored Casio calculator on desk in Electrical Lab",
                "Electrical Lab 2 Desk 5",
                LocalDate.now()
        );
        FoundItemResponse foundItem = foundItemService.createFoundItem(foundReq, staffEmail);

        List<MatchSuggestionDto> matches = matchingService.findMatchesForLostReport(lostReport.getId());

        assertFalse(matches.isEmpty());
        MatchSuggestionDto bestMatch = matches.getFirst();
        assertEquals(foundItem.getId(), bestMatch.getFoundItemId());
        assertTrue(bestMatch.getScore() >= 70, "Score should be >= 70 due to matching category and strong keyword overlap");
        assertTrue(bestMatch.isCategoryMatched());
    }

    @Test
    void testConfirmMatchUpdatesBothStatuses() {
        LostReportRequest lostReq = new LostReportRequest(
                "Steel Water Flask Milton",
                categoryId,
                "Milton thermosteel flask 1000ml silver color",
                "Auditorium Row 4",
                LocalDate.now()
        );
        LostReportResponse lostReport = lostReportService.createReport(lostReq, student1Email);

        FoundItemRequest foundReq = new FoundItemRequest(
                "Milton Thermosteel Silver Flask",
                categoryId,
                "Milton silver steel flask found on chair",
                "Auditorium",
                LocalDate.now()
        );
        FoundItemResponse foundItem = foundItemService.createFoundItem(foundReq, staffEmail);

        matchingService.confirmMatch(lostReport.getId(), foundItem.getId(), student1Email);

        LostReportResponse updatedLost = lostReportService.getReportById(lostReport.getId());
        FoundItemResponse updatedFound = foundItemService.getFoundItemById(foundItem.getId());

        assertEquals(ReportStatus.MATCHED, updatedLost.getStatus());
        assertEquals(ItemStatus.CLAIMED, updatedFound.getStatus());
    }
}

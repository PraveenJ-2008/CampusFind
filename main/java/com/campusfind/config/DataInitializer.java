package com.campusfind.config;

import com.campusfind.entity.*;
import com.campusfind.repository.CategoryRepository;
import com.campusfind.repository.FoundItemRepository;
import com.campusfind.repository.LostReportRepository;
import com.campusfind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LostReportRepository lostReportRepository;
    private final FoundItemRepository foundItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@campusfind.edu}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.name:Campus Administrator}")
    private String adminName;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           LostReportRepository lostReportRepository,
                           FoundItemRepository foundItemRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.lostReportRepository = lostReportRepository;
        this.foundItemRepository = foundItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Checking and seeding initial CampusFind demo data...");
        seedCategories();
        User admin = seedAdminUser();
        User student = seedStudentUser();
        User staff = seedStaffUser();
        seedDemoItems(student, staff);
        log.info("CampusFind initialization complete.");
    }

    private void seedCategories() {
        Map<String, String> defaultCategories = new LinkedHashMap<>();
        defaultCategories.put("ID Card", "Student and Faculty College Identity cards");
        defaultCategories.put("Electronics", "Laptops, Chargers, Headphones, USB drives, Phones");
        defaultCategories.put("Calculator", "Scientific and Financial calculators");
        defaultCategories.put("Water Bottle", "Steel, plastic and insulated drinkware");
        defaultCategories.put("Books", "Course textbooks, notebooks, library records");
        defaultCategories.put("Stationery", "Pencil boxes, geometry sets, folders");
        defaultCategories.put("Accessories", "Watches, wallets, keys, umbrellas, glasses");
        defaultCategories.put("Others", "Miscellaneous campus belongings");

        for (Map.Entry<String, String> entry : defaultCategories.entrySet()) {
            if (!categoryRepository.existsByNameIgnoreCase(entry.getKey())) {
                categoryRepository.save(new Category(entry.getKey(), entry.getValue(), true));
            }
        }
    }

    private User seedAdminUser() {
        return userRepository.findByEmail(adminEmail.toLowerCase())
                .orElseGet(() -> {
                    User admin = new User(
                            adminName,
                            adminEmail.toLowerCase(),
                            passwordEncoder.encode(adminPassword),
                            Role.ADMIN
                    );
                    log.info("Default Admin account created: {}", adminEmail);
                    return userRepository.save(admin);
                });
    }

    private User seedStudentUser() {
        String studentEmail = "student@campusfind.edu";
        return userRepository.findByEmail(studentEmail)
                .orElseGet(() -> {
                    User student = new User(
                            "Praveen J",
                            studentEmail,
                            passwordEncoder.encode("Student@123"),
                            Role.STUDENT
                    );
                    log.info("Demo Student account created: {}", studentEmail);
                    return userRepository.save(student);
                });
    }

    private User seedStaffUser() {
        String staffEmail = "staff@campusfind.edu";
        return userRepository.findByEmail(staffEmail)
                .orElseGet(() -> {
                    User staff = new User(
                            "Campus Security Desk",
                            staffEmail,
                            passwordEncoder.encode("Staff@123"),
                            Role.STAFF
                    );
                    log.info("Demo Staff account created: {}", staffEmail);
                    return userRepository.save(staff);
                });
    }

    private void seedDemoItems(User student, User staff) {
        if (lostReportRepository.count() == 0 && foundItemRepository.count() == 0) {
            Category calculatorCat = categoryRepository.findByNameIgnoreCase("Calculator").orElse(null);
            Category idCardCat = categoryRepository.findByNameIgnoreCase("ID Card").orElse(null);
            Category electronicsCat = categoryRepository.findByNameIgnoreCase("Electronics").orElse(null);

            if (calculatorCat != null && idCardCat != null && electronicsCat != null) {
                // Demo Lost Reports
                LostReport lostCalc = new LostReport(
                        "Blue Casio FX-991EX Scientific Calculator",
                        "Left my Casio FX-991EX scientific calculator on desk in Central Library 2nd floor reading hall.",
                        "Central Library 2nd Floor",
                        LocalDate.now().minusDays(1),
                        student,
                        calculatorCat
                );
                lostReportRepository.save(lostCalc);

                LostReport lostId = new LostReport(
                        "College Student ID Card - Praveen J",
                        "Lost my official College ID card with CSE department lanyard near Main Cafeteria food court.",
                        "Main Cafeteria Block B",
                        LocalDate.now().minusDays(2),
                        student,
                        idCardCat
                );
                lostReportRepository.save(lostId);

                LostReport lostCharger = new LostReport(
                        "Black Dell 65W Laptop Charger",
                        "Forgot original black Dell Type-C laptop charger on workstation table.",
                        "Computer Lab 3",
                        LocalDate.now().minusDays(3),
                        student,
                        electronicsCat
                );
                lostReportRepository.save(lostCharger);

                // Demo Found Items
                FoundItem foundCalc = new FoundItem(
                        "Casio FX-991EX Scientific Calculator Blue",
                        "Found a blue colored Casio FX-991EX scientific calculator on study table in Library hall.",
                        "Central Library Reading Hall",
                        LocalDate.now().minusDays(1),
                        staff,
                        calculatorCat
                );
                foundItemRepository.save(foundCalc);

                FoundItem foundId = new FoundItem(
                        "Student ID Card Praveen CSE Department",
                        "Student identity card picked up from cafeteria billing counter with blue lanyard.",
                        "Main Cafeteria Counter",
                        LocalDate.now().minusDays(2),
                        staff,
                        idCardCat
                );
                foundItemRepository.save(foundId);

                FoundItem foundCharger = new FoundItem(
                        "Dell 65W Laptop Power Adapter",
                        "Dell black power adapter left connected to wall socket at Computer Lab 3.",
                        "Computer Lab 3 Wall Socket",
                        LocalDate.now().minusDays(3),
                        staff,
                        electronicsCat
                );
                foundItemRepository.save(foundCharger);

                log.info("Demo Lost Reports and Found Items created with matching pairs.");
            }
        }
    }
}

package com.campusfind.service;

import com.campusfind.dto.AuthResponse;
import com.campusfind.dto.LoginRequest;
import com.campusfind.dto.RegisterRequest;
import com.campusfind.dto.UserDto;
import com.campusfind.entity.Role;
import com.campusfind.exception.BadRequestException;
import com.campusfind.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testStudentRegistrationSuccess() {
        RegisterRequest request = new RegisterRequest("Alice Student", "alice@campusfind.edu", "Password@123", Role.STUDENT);
        UserDto userDto = authService.register(request);

        assertNotNull(userDto);
        assertNotNull(userDto.getId());
        assertEquals("alice@campusfind.edu", userDto.getEmail());
        assertEquals(Role.STUDENT, userDto.getRole());
    }

    @Test
    void testStaffRegistrationSuccess() {
        RegisterRequest request = new RegisterRequest("Bob Staff", "bob@campusfind.edu", "Password@123", Role.STAFF);
        UserDto userDto = authService.register(request);

        assertNotNull(userDto);
        assertEquals(Role.STAFF, userDto.getRole());
    }

    @Test
    void testSelfRegistrationAsAdminIsRejected() {
        RegisterRequest request = new RegisterRequest("Hacker Admin", "hacker@campusfind.edu", "Password@123", Role.ADMIN);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("ADMIN"));
    }

    @Test
    void testDuplicateEmailRejected() {
        RegisterRequest request1 = new RegisterRequest("User One", "duplicate@campusfind.edu", "Password@123", Role.STUDENT);
        authService.register(request1);

        RegisterRequest request2 = new RegisterRequest("User Two", "duplicate@campusfind.edu", "Password@456", Role.STUDENT);
        assertThrows(ConflictException.class, () -> authService.register(request2));
    }

    @Test
    void testLoginSuccess() {
        RegisterRequest request = new RegisterRequest("Charlie Student", "charlie@campusfind.edu", "MySecret@123", Role.STUDENT);
        authService.register(request);

        LoginRequest loginRequest = new LoginRequest("charlie@campusfind.edu", "MySecret@123");
        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertEquals("charlie@campusfind.edu", response.getUser().getEmail());
    }

    @Test
    void testLoginWithWrongPasswordThrowsBadCredentials() {
        RegisterRequest request = new RegisterRequest("David Student", "david@campusfind.edu", "Correct@123", Role.STUDENT);
        authService.register(request);

        LoginRequest loginRequest = new LoginRequest("david@campusfind.edu", "WrongPassword@123");
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}

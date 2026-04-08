package com.example.zuora.service;

import com.example.zuora.dto.SignupRequest;
import com.example.zuora.model.User;
import com.example.zuora.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "zuora.client-id=test",
    "zuora.client-secret=test"
})
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_Success() throws Exception {
        // Create signup request
        SignupRequest request = new SignupRequest();
        request.setEmail("test.user@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPhone("1234567890");
        request.setAddress1("123 Main St");
        request.setCity("San Francisco");
        request.setState("CA");
        request.setZipCode("94105");
        request.setCountry("USA");

        // Execute
        SignupResult result = userService.createUser(request);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getUser());
        User user = result.getUser();
        assertEquals("test.user@example.com", user.getEmail());
        assertEquals("Test", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertEquals(User.Role.CUSTOMER, user.getRole());
        assertTrue(user.getIsActive());

        // Verify saved in database
        User savedUser = userRepository.findByEmail("test.user@example.com").orElse(null);
        assertNotNull(savedUser);
    }

    @Test
    void createUser_DuplicateEmail() throws Exception {
        // Create first user
        SignupRequest request = new SignupRequest();
        request.setEmail("duplicate@example.com");
        request.setPassword("Password123!");
        request.setFirstName("First");
        request.setLastName("User");
        request.setPhone("1234567890");
        request.setAddress1("123 Main St");
        request.setCity("San Francisco");
        request.setState("CA");
        request.setZipCode("94105");
        request.setCountry("USA");

        userService.createUser(request);

        // Try to create duplicate - note: result ignored, just to create user
        SignupRequest duplicateRequest = new SignupRequest();
        duplicateRequest.setEmail("duplicate@example.com");
        duplicateRequest.setPassword("Password123!");
        duplicateRequest.setFirstName("Second");
        duplicateRequest.setLastName("User");
        duplicateRequest.setPhone("0987654321");
        duplicateRequest.setAddress1("456 Other St");
        duplicateRequest.setCity("New York");
        duplicateRequest.setState("NY");
        duplicateRequest.setZipCode("10001");
        duplicateRequest.setCountry("USA");

        // Execute & Verify
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(duplicateRequest);
        });
        assertEquals("Email already registered: duplicate@example.com", exception.getMessage());
    }

    @Test
    void getUserById_Success() throws Exception {
        // Create user first
        SignupRequest request = new SignupRequest();
        request.setEmail("find.by.id@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Find");
        request.setLastName("ById");
        request.setPhone("1234567890");
        request.setAddress1("123 Main St");
        request.setCity("San Francisco");
        request.setState("CA");
        request.setZipCode("94105");
        request.setCountry("USA");

        SignupResult created = userService.createUser(request);

        // Execute
        User result = userService.getUserById(created.getUser().getId());

        // Verify
        assertNotNull(result);
        assertEquals(created.getUser().getId(), result.getId());
        assertEquals("find.by.id@example.com", result.getEmail());
    }

    @Test
    void getUserById_NotFound() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(99999L);
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void updateUserProfile_Success() throws Exception {
        // Create user first
        SignupRequest request = new SignupRequest();
        request.setEmail("update.profile@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Original");
        request.setLastName("Name");
        request.setPhone("1234567890");
        request.setAddress1("123 Main St");
        request.setCity("San Francisco");
        request.setState("CA");
        request.setZipCode("94105");
        request.setCountry("USA");

        SignupResult created = userService.createUser(request);

        // Execute
        userService.updateUserProfile(created.getUser().getId(), "Updated", "Profile", "5555555555");

        // Verify
        User updated = userService.getUserById(created.getUser().getId());
        assertEquals("Updated", updated.getFirstName());
        assertEquals("Profile", updated.getLastName());
        assertEquals("5555555555", updated.getPhone());
    }

    @Test
    void createAdminUser_Success() {
        // Execute
        User admin = userService.createAdminUser("admin.test@example.com", "adminpass123", "Admin", "Test");

        // Verify
        assertNotNull(admin);
        assertEquals("admin.test@example.com", admin.getEmail());
        assertEquals("Admin", admin.getFirstName());
        assertEquals(User.Role.ADMIN, admin.getRole());
    }
}

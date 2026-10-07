package com.odas.auth;

import com.odas.User;
import com.odas.util.PasswordUtil;
import com.odas.util.ValidationUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for authentication, password hashing security, and ValidationUtil rules.
 */
public class AuthValidationTest {

    @Test
    public void testPasswordHashingSecurity() {
        String password = "securePassword123!";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertNotNull(hash1);
        assertNotNull(hash2);
        // PBKDF2 uses a cryptographically random salt per hash:
        assertNotEquals("PBKDF2 hashes should differ due to unique salts", hash1, hash2);
        assertTrue(hash1.startsWith(PasswordUtil.PREFIX));
        assertTrue(hash2.startsWith(PasswordUtil.PREFIX));

        // Both verify correctly against the original password
        assertTrue(PasswordUtil.checkPassword(password, hash1));
        assertTrue(PasswordUtil.checkPassword(password, hash2));
        assertFalse(PasswordUtil.checkPassword("WrongPassword", hash1));
        assertFalse(PasswordUtil.checkPassword("", hash1));
        assertFalse(PasswordUtil.checkPassword(null, hash1));

        // Verify legacy SHA-256 fallback and upgrade detection
        String legacySha256 = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"; // "password123"
        assertTrue(PasswordUtil.checkPassword("password123", legacySha256));
        assertFalse(PasswordUtil.checkPassword("wrongpass", legacySha256));
        assertTrue(PasswordUtil.needsUpgrade(legacySha256));
        assertFalse(PasswordUtil.needsUpgrade(hash1));
    }

    @Test
    public void testRoleAssignments() {
        User patientUser = new User(1, "patient_jane", "hashed_pwd", "PATIENT");
        User doctorUser = new User(2, "dr_smith", "hashed_pwd", "DOCTOR");
        User adminUser = new User(3, "admin", "hashed_pwd", "ADMIN");

        assertEquals("PATIENT", patientUser.getRole());
        assertEquals("DOCTOR", doctorUser.getRole());
        assertEquals("ADMIN", adminUser.getRole());
    }

    @Test
    public void testAgeValidationRules() {
        // Valid ages using real ValidationUtil method
        assertTrue(ValidationUtil.isValidAge(1));
        assertTrue(ValidationUtil.isValidAge(25));
        assertTrue(ValidationUtil.isValidAge(120));

        // Invalid ages
        assertFalse(ValidationUtil.isValidAge(0));
        assertFalse(ValidationUtil.isValidAge(-1));
        assertFalse(ValidationUtil.isValidAge(121));
        assertFalse(ValidationUtil.isValidAge(200));
    }

    @Test
    public void testPatientProfileValidation() {
        // Valid profile
        String error = ValidationUtil.validatePatientProfile("Alice Smith", "30", "Female", "9876543210", "alice@example.com", "123 Main St");
        assertNull("Valid profile should produce no validation errors", error);

        // Blank name
        assertNotNull(ValidationUtil.validatePatientProfile("", "30", "Female", "9876543210", "alice@example.com", "123 Main St"));
        // Invalid age
        assertNotNull(ValidationUtil.validatePatientProfile("Alice", "150", "Female", "9876543210", "alice@example.com", "123 Main St"));
        assertNotNull(ValidationUtil.validatePatientProfile("Alice", "abc", "Female", "9876543210", "alice@example.com", "123 Main St"));
        // Invalid gender
        assertNotNull(ValidationUtil.validatePatientProfile("Alice", "30", "Unknown", "9876543210", "alice@example.com", "123 Main St"));
        // Invalid phone
        assertNotNull(ValidationUtil.validatePatientProfile("Alice", "30", "Female", "12345", "alice@example.com", "123 Main St"));
        // Invalid email
        assertNotNull(ValidationUtil.validatePatientProfile("Alice", "30", "Female", "9876543210", "not-an-email", "123 Main St"));
    }

    @Test
    public void testDoctorValidationRules() {
        assertTrue(ValidationUtil.isValidExperience(0));
        assertTrue(ValidationUtil.isValidExperience(15));
        assertTrue(ValidationUtil.isValidExperience(60));
        assertFalse(ValidationUtil.isValidExperience(-1));
        assertFalse(ValidationUtil.isValidExperience(61));

        assertTrue(ValidationUtil.isValidAvailableTime("09:00-13:00"));
        assertTrue(ValidationUtil.isValidAvailableTime("14:30-18:00"));
        assertFalse(ValidationUtil.isValidAvailableTime("18:00-14:00")); // start > end
        assertFalse(ValidationUtil.isValidAvailableTime("invalid"));

        assertTrue(ValidationUtil.isValidAvailableDays("Mon,Wed,Fri"));
        assertTrue(ValidationUtil.isValidAvailableDays("Mon,Tue,Wed,Thu,Fri,Sat,Sun"));
        assertFalse(ValidationUtil.isValidAvailableDays("Monday,Friday"));
        assertFalse(ValidationUtil.isValidAvailableDays(""));

        assertNull(ValidationUtil.validateConsultationFee("650"));
        assertNull(ValidationUtil.validateConsultationFee("650.50"));
        assertNotNull(ValidationUtil.validateConsultationFee("0"));
        assertNotNull(ValidationUtil.validateConsultationFee("-5"));
        assertNotNull(ValidationUtil.validateConsultationFee("abc"));
    }
}

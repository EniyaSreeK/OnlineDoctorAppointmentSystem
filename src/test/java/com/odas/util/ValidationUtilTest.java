package com.odas.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for ValidationUtil.
 * 
 * Validates:
 * - Patient profile validation (name, age 1-120, gender, 10-digit phone, email)
 * - Doctor profile validation (name, specialization, experience 0-60, fee > 0 and <= 999999.99, time range, days)
 * - Password change validation (PBKDF2 hash verification, min 8 chars, difference from current, match confirmation)
 */
public class ValidationUtilTest {

    // =========================================================================
    // Patient Validations
    // =========================================================================

    @Test
    public void testIsValidName() {
        assertTrue(ValidationUtil.isValidName("John Doe"));
        assertTrue(ValidationUtil.isValidName("A"));
        assertTrue(ValidationUtil.isValidName("Dr. Jane Smith, M.D."));

        assertFalse(ValidationUtil.isValidName(null));
        assertFalse(ValidationUtil.isValidName(""));
        assertFalse(ValidationUtil.isValidName("   "));
    }

    @Test
    public void testIsValidAge() {
        assertTrue("Min boundary age 1 must be valid", ValidationUtil.isValidAge(1));
        assertTrue("Adult age 35 must be valid", ValidationUtil.isValidAge(35));
        assertTrue("Max boundary age 120 must be valid", ValidationUtil.isValidAge(120));

        assertFalse("Age 0 must be invalid", ValidationUtil.isValidAge(0));
        assertFalse("Negative age must be invalid", ValidationUtil.isValidAge(-1));
        assertFalse("Age 121 must be invalid", ValidationUtil.isValidAge(121));
        assertFalse("Age 999 must be invalid", ValidationUtil.isValidAge(999));
    }

    @Test
    public void testIsValidGender() {
        assertTrue(ValidationUtil.isValidGender("MALE"));
        assertTrue(ValidationUtil.isValidGender("FEMALE"));
        assertTrue(ValidationUtil.isValidGender("OTHER"));
        assertTrue(ValidationUtil.isValidGender("male"));
        assertTrue(ValidationUtil.isValidGender("Female"));
        assertTrue(ValidationUtil.isValidGender("  Other  "));

        assertFalse(ValidationUtil.isValidGender(null));
        assertFalse(ValidationUtil.isValidGender(""));
        assertFalse(ValidationUtil.isValidGender("UNKNOWN"));
        assertFalse(ValidationUtil.isValidGender("X"));
    }

    @Test
    public void testIsValidPhone() {
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
        assertTrue(ValidationUtil.isValidPhone("0123456789"));
        assertTrue(ValidationUtil.isValidPhone("  9876543210  "));

        assertFalse(ValidationUtil.isValidPhone(null));
        assertFalse(ValidationUtil.isValidPhone(""));
        assertFalse(ValidationUtil.isValidPhone("123456789")); // 9 digits
        assertFalse(ValidationUtil.isValidPhone("12345678901")); // 11 digits
        assertFalse(ValidationUtil.isValidPhone("98765abcde")); // alphanumeric
        assertFalse(ValidationUtil.isValidPhone("98765-43210")); // hyphenated
    }

    @Test
    public void testIsValidEmail() {
        assertTrue(ValidationUtil.isValidEmail("user@example.com"));
        assertTrue(ValidationUtil.isValidEmail("john.doe@clinic.org"));
        assertTrue(ValidationUtil.isValidEmail("patient+tag@hospital.co.in"));

        assertFalse(ValidationUtil.isValidEmail(null));
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail("not-an-email"));
        assertFalse(ValidationUtil.isValidEmail("@missingusername.com"));
        assertFalse(ValidationUtil.isValidEmail("missingdomain@.com"));
        assertFalse(ValidationUtil.isValidEmail("missingat.com"));
    }

    @Test
    public void testValidatePatientProfileCompleteScenarios() {
        // Valid profile
        assertNull(ValidationUtil.validatePatientProfile("Alice Smith", "30", "Female", "9876543210", "alice@example.com", "123 Main St"));

        // Name missing
        assertEquals("Patient full name is required.",
                ValidationUtil.validatePatientProfile("", "30", "Female", "9876543210", "alice@example.com", "123 Main St"));
        assertEquals("Patient full name is required.",
                ValidationUtil.validatePatientProfile(null, "30", "Female", "9876543210", "alice@example.com", "123 Main St"));

        // Age missing or invalid
        assertEquals("Age is required.",
                ValidationUtil.validatePatientProfile("Alice", "", "Female", "9876543210", "alice@example.com", "123 Main St"));
        assertEquals("Age is required.",
                ValidationUtil.validatePatientProfile("Alice", null, "Female", "9876543210", "alice@example.com", "123 Main St"));
        assertEquals("Age must be a valid whole number.",
                ValidationUtil.validatePatientProfile("Alice", "abc", "Female", "9876543210", "alice@example.com", "123 Main St"));
        assertEquals("Age must be between 1 and 120.",
                ValidationUtil.validatePatientProfile("Alice", "0", "Female", "9876543210", "alice@example.com", "123 Main St"));
        assertEquals("Age must be between 1 and 120.",
                ValidationUtil.validatePatientProfile("Alice", "121", "Female", "9876543210", "alice@example.com", "123 Main St"));

        // Gender invalid
        assertEquals("Please select a valid gender (Male, Female, or Other).",
                ValidationUtil.validatePatientProfile("Alice", "30", "Alien", "9876543210", "alice@example.com", "123 Main St"));

        // Phone invalid
        assertEquals("Phone number must be exactly 10 digits.",
                ValidationUtil.validatePatientProfile("Alice", "30", "Female", "123", "alice@example.com", "123 Main St"));

        // Email invalid
        assertEquals("Please enter a valid email address (e.g. user@example.com).",
                ValidationUtil.validatePatientProfile("Alice", "30", "Female", "9876543210", "bad-email", "123 Main St"));
    }

    // =========================================================================
    // Doctor Validations
    // =========================================================================

    @Test
    public void testIsValidExperience() {
        assertTrue(ValidationUtil.isValidExperience(0));
        assertTrue(ValidationUtil.isValidExperience(1));
        assertTrue(ValidationUtil.isValidExperience(30));
        assertTrue(ValidationUtil.isValidExperience(60));

        assertFalse(ValidationUtil.isValidExperience(-1));
        assertFalse(ValidationUtil.isValidExperience(61));
        assertFalse(ValidationUtil.isValidExperience(100));
    }

    @Test
    public void testIsValidConsultationFee() {
        assertTrue(ValidationUtil.isValidConsultationFee(0.01));
        assertTrue(ValidationUtil.isValidConsultationFee(500.0));
        assertTrue(ValidationUtil.isValidConsultationFee(ValidationUtil.MAX_CONSULTATION_FEE));

        assertFalse(ValidationUtil.isValidConsultationFee(0.0));
        assertFalse(ValidationUtil.isValidConsultationFee(-50.0));
        assertFalse(ValidationUtil.isValidConsultationFee(ValidationUtil.MAX_CONSULTATION_FEE + 0.01));
    }

    @Test
    public void testValidateConsultationFeeString() {
        assertNull(ValidationUtil.validateConsultationFee("500"));
        assertNull(ValidationUtil.validateConsultationFee("650.50"));
        assertNull(ValidationUtil.validateConsultationFee("999999.99"));

        assertEquals("Consultation fee is required.", ValidationUtil.validateConsultationFee(null));
        assertEquals("Consultation fee is required.", ValidationUtil.validateConsultationFee("   "));
        assertEquals("Consultation fee must be greater than 0.", ValidationUtil.validateConsultationFee("0"));
        assertEquals("Consultation fee must be greater than 0.", ValidationUtil.validateConsultationFee("-100"));
        assertEquals("Consultation fee cannot exceed 999,999.99.", ValidationUtil.validateConsultationFee("1000000"));
        assertEquals("Consultation fee must be a valid numeric amount.", ValidationUtil.validateConsultationFee("five hundred"));
    }

    @Test
    public void testIsValidAvailableTime() {
        assertTrue(ValidationUtil.isValidAvailableTime("09:00-13:00"));
        assertTrue(ValidationUtil.isValidAvailableTime("14:30-18:00"));
        assertTrue(ValidationUtil.isValidAvailableTime("00:00-23:59"));

        assertFalse(ValidationUtil.isValidAvailableTime(null));
        assertFalse(ValidationUtil.isValidAvailableTime(""));
        assertFalse(ValidationUtil.isValidAvailableTime("9:00-13:00")); // missing leading zero
        assertFalse(ValidationUtil.isValidAvailableTime("13:00-09:00")); // start > end
        assertFalse(ValidationUtil.isValidAvailableTime("10:00-10:00")); // start == end
        assertFalse(ValidationUtil.isValidAvailableTime("25:00-26:00")); // invalid hours
        assertFalse(ValidationUtil.isValidAvailableTime("09:65-17:00")); // invalid minutes
        assertFalse(ValidationUtil.isValidAvailableTime("invalid"));
    }

    @Test
    public void testIsValidAvailableDays() {
        assertTrue(ValidationUtil.isValidAvailableDays("Mon"));
        assertTrue(ValidationUtil.isValidAvailableDays("MON"));
        assertTrue(ValidationUtil.isValidAvailableDays("Mon,Wed,Fri"));
        assertTrue(ValidationUtil.isValidAvailableDays("mon, wed , fri"));
        assertTrue(ValidationUtil.isValidAvailableDays("Mon,Tue,Wed,Thu,Fri,Sat,Sun"));

        assertFalse(ValidationUtil.isValidAvailableDays(null));
        assertFalse(ValidationUtil.isValidAvailableDays(""));
        assertFalse(ValidationUtil.isValidAvailableDays("Monday"));
        assertFalse(ValidationUtil.isValidAvailableDays("Mon,Funday,Fri"));
    }

    @Test
    public void testValidateDoctorProfileCompleteScenarios() {
        // Valid profile
        assertNull(ValidationUtil.validateDoctorProfile(
                "Dr. Robert", "Cardiology", "MD", "15", "9876543210", "doc@clinic.com", "Apollo Hospital",
                "Mon,Wed,Fri", "09:00-17:00", "750"));

        // Name missing
        assertEquals("Doctor name is required.",
                ValidationUtil.validateDoctorProfile("", "Cardiology", "MD", "15", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Wed,Fri", "09:00-17:00", "750"));

        // Specialization missing
        assertEquals("Specialization is required.",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "", "MD", "15", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Wed,Fri", "09:00-17:00", "750"));

        // Experience invalid
        assertEquals("Experience must be between 0 and 60 years.",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "65", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Wed,Fri", "09:00-17:00", "750"));
        assertEquals("Experience must be a valid whole number.",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "abc", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Wed,Fri", "09:00-17:00", "750"));

        // Phone invalid
        assertEquals("Phone number must be exactly 10 digits.",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "15", "98765", "doc@clinic.com", "Apollo Hospital", "Mon,Wed,Fri", "09:00-17:00", "750"));

        // Email invalid
        assertEquals("Please enter a valid email address.",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "15", "9876543210", "notanemail", "Apollo Hospital", "Mon,Wed,Fri", "09:00-17:00", "750"));

        // Days invalid
        assertEquals("Available days must be comma-separated days from Mon-Sun (e.g. 'Mon,Wed,Fri').",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "15", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Funday", "09:00-17:00", "750"));

        // Time invalid
        assertEquals("Available time must be in 24-hr 'HH:mm-HH:mm' format with start before end (e.g. '09:00-13:00').",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "15", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Wed", "17:00-09:00", "750"));

        // Fee invalid
        assertEquals("Consultation fee must be greater than 0.",
                ValidationUtil.validateDoctorProfile("Dr. Robert", "Cardiology", "MD", "15", "9876543210", "doc@clinic.com", "Apollo Hospital", "Mon,Wed", "09:00-17:00", "0"));
    }

    // =========================================================================
    // Password Change Validations
    // =========================================================================

    @Test
    public void testValidatePasswordChangeScenarios() {
        String currentPlain = "CurrentP@ss123";
        String currentHash = PasswordUtil.hashPassword(currentPlain);

        // Valid change
        assertNull(ValidationUtil.validatePasswordChange(currentPlain, currentHash, "NewP@ssword2026", "NewP@ssword2026"));

        // Current password null or empty
        assertEquals("Current password is required.",
                ValidationUtil.validatePasswordChange(null, currentHash, "NewP@ssword2026", "NewP@ssword2026"));
        assertEquals("Current password is required.",
                ValidationUtil.validatePasswordChange("", currentHash, "NewP@ssword2026", "NewP@ssword2026"));

        // Incorrect current password
        assertEquals("Incorrect current password.",
                ValidationUtil.validatePasswordChange("WrongPassword999", currentHash, "NewP@ssword2026", "NewP@ssword2026"));

        // New password too short (< 8 chars)
        assertEquals("New password must be at least 8 characters long.",
                ValidationUtil.validatePasswordChange(currentPlain, currentHash, "short", "short"));

        // New password identical to current password
        assertEquals("New password cannot be the same as your current password.",
                ValidationUtil.validatePasswordChange(currentPlain, currentHash, currentPlain, currentPlain));

        // Confirm password mismatch
        assertEquals("New password and confirm password do not match.",
                ValidationUtil.validatePasswordChange(currentPlain, currentHash, "NewP@ssword2026", "DifferentP@ss2026"));
        assertEquals("New password and confirm password do not match.",
                ValidationUtil.validatePasswordChange(currentPlain, currentHash, "NewP@ssword2026", null));
    }
}

package com.odas.auth;

import com.odas.util.PasswordUtil;
import com.odas.util.ValidationUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for STAGE 2 — Validation Logic & Password Change.
 */
public class Stage2ProfileAndPasswordTest {

    // =========================================================================
    // Patient Profile Validation Tests
    // =========================================================================

    @Test
    public void testValidPatientProfile() {
        String err = ValidationUtil.validatePatientProfile(
                "Jane Doe", "30", "Female", "9876543210", "jane@example.com", "123 Main St");
        assertNull("Valid patient data should return no error", err);
    }

    @Test
    public void testPatientProfileEmptyName() {
        String err = ValidationUtil.validatePatientProfile(
                "   ", "30", "Female", "9876543210", "jane@example.com", "123 Main St");
        assertNotNull(err);
        assertTrue(err.toLowerCase().contains("name is required"));
    }

    @Test
    public void testPatientProfileInvalidAge() {
        // Age below 1
        String errUnder = ValidationUtil.validatePatientProfile(
                "Jane Doe", "0", "Female", "9876543210", "jane@example.com", "123 Main St");
        assertNotNull(errUnder);
        assertTrue(errUnder.contains("1 and 120"));

        // Age above 120
        String errOver = ValidationUtil.validatePatientProfile(
                "Jane Doe", "125", "Female", "9876543210", "jane@example.com", "123 Main St");
        assertNotNull(errOver);
        assertTrue(errOver.contains("1 and 120"));

        // Non-numeric age
        String errNonNum = ValidationUtil.validatePatientProfile(
                "Jane Doe", "abc", "Female", "9876543210", "jane@example.com", "123 Main St");
        assertNotNull(errNonNum);
        assertTrue(errNonNum.contains("valid whole number"));
    }

    @Test
    public void testPatientProfileInvalidGender() {
        String err = ValidationUtil.validatePatientProfile(
                "Jane Doe", "25", "UnknownGender", "9876543210", "jane@example.com", "123 Main St");
        assertNotNull(err);
        assertTrue(err.contains("valid gender"));
    }

    @Test
    public void testPatientProfileValidGenders() {
        assertNull(ValidationUtil.validatePatientProfile("Jane", "25", "Male", "9876543210", "j@e.com", ""));
        assertNull(ValidationUtil.validatePatientProfile("Jane", "25", "Female", "9876543210", "j@e.com", ""));
        assertNull(ValidationUtil.validatePatientProfile("Jane", "25", "Other", "9876543210", "j@e.com", ""));
    }

    @Test
    public void testPatientProfileInvalidPhone() {
        // 9 digits
        String errShort = ValidationUtil.validatePatientProfile(
                "Jane Doe", "25", "Female", "987654321", "jane@example.com", "");
        assertNotNull(errShort);
        assertTrue(errShort.contains("10 digits"));

        // 11 digits
        String errLong = ValidationUtil.validatePatientProfile(
                "Jane Doe", "25", "Female", "98765432100", "jane@example.com", "");
        assertNotNull(errLong);
        assertTrue(errLong.contains("10 digits"));

        // Non-numeric
        String errLetters = ValidationUtil.validatePatientProfile(
                "Jane Doe", "25", "Female", "98765abcde", "jane@example.com", "");
        assertNotNull(errLetters);
        assertTrue(errLetters.contains("10 digits"));
    }

    @Test
    public void testPatientProfileInvalidEmail() {
        String err1 = ValidationUtil.validatePatientProfile(
                "Jane Doe", "25", "Female", "9876543210", "not-an-email", "");
        assertNotNull(err1);
        assertTrue(err1.contains("valid email"));

        String err2 = ValidationUtil.validatePatientProfile(
                "Jane Doe", "25", "Female", "9876543210", "user@nodomain", "");
        assertNotNull(err2);
        assertTrue(err2.contains("valid email"));
    }

    // =========================================================================
    // Doctor Edit Validation Tests
    // =========================================================================

    @Test
    public void testValidDoctorProfile() {
        String err = ValidationUtil.validateDoctorProfile(
                "Dr. Sarah", "Cardiology", "MBBS, MD", "15", "9876543210",
                "sarah@medicare.com", "Building A", "Mon,Wed,Fri", "09:00-14:00", "750");
        assertNull("Valid doctor data should return no error", err);
    }

    @Test
    public void testDoctorExperienceBoundaries() {
        // Experience < 0
        String errUnder = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "-1", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "10:00-12:00", "500");
        assertNotNull(errUnder);
        assertTrue(errUnder.contains("0 and 60"));

        // Experience > 60
        String errOver = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "65", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "10:00-12:00", "500");
        assertNotNull(errOver);
        assertTrue(errOver.contains("0 and 60"));
    }

    @Test
    public void testDoctorAvailableTimeFormatAndOrder() {
        // Invalid format
        String errFmt = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "5", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "9am-1pm", "500");
        assertNotNull(errFmt);
        assertTrue(errFmt.contains("HH:mm-HH:mm"));

        // Start time after end time (14:00-10:00)
        String errOrder = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "5", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "14:00-10:00", "500");
        assertNotNull(errOrder);
        assertTrue(errOrder.contains("start before end"));

        // Valid 24-hr time
        assertTrue(ValidationUtil.isValidAvailableTime("09:00-17:00"));
        assertTrue(ValidationUtil.isValidAvailableTime("08:30-12:30"));
        assertFalse(ValidationUtil.isValidAvailableTime("12:00-12:00")); // start equals end
    }

    @Test
    public void testDoctorAvailableDaysValidation() {
        assertTrue(ValidationUtil.isValidAvailableDays("Mon,Tue,Wed"));
        assertTrue(ValidationUtil.isValidAvailableDays("Fri,Sat,Sun"));
        assertTrue(ValidationUtil.isValidAvailableDays("Mon"));
        assertFalse(ValidationUtil.isValidAvailableDays("Monday,Tuesday")); // Needs Mon,Tue abbr
        assertFalse(ValidationUtil.isValidAvailableDays("InvalidDay"));
    }

    @Test
    public void testConsultationFeeValidation() {
        // Valid amounts: 650, 650.50, 1000
        assertNull("Fee 650 should be valid", ValidationUtil.validateConsultationFee("650"));
        assertNull("Fee 650.50 should be valid", ValidationUtil.validateConsultationFee("650.50"));
        assertNull("Fee 1000 should be valid", ValidationUtil.validateConsultationFee("1000"));

        // Invalid: 0
        String errZero = ValidationUtil.validateConsultationFee("0");
        assertNotNull("Fee 0 should be invalid", errZero);
        assertTrue(errZero.contains("greater than 0"));

        // Invalid: -5
        String errNeg = ValidationUtil.validateConsultationFee("-5");
        assertNotNull("Fee -5 should be invalid", errNeg);
        assertTrue(errNeg.contains("greater than 0"));

        // Invalid: 'abc'
        String errAbc = ValidationUtil.validateConsultationFee("abc");
        assertNotNull("Fee 'abc' should be invalid", errAbc);
        assertTrue(errAbc.contains("valid numeric amount"));

        // Invalid: empty / null
        String errEmpty = ValidationUtil.validateConsultationFee("");
        assertNotNull("Empty fee should be invalid", errEmpty);
        assertTrue(errEmpty.contains("required"));

        String errWhitespace = ValidationUtil.validateConsultationFee("   ");
        assertNotNull("Whitespace fee should be invalid", errWhitespace);
        assertTrue(errWhitespace.contains("required"));

        String errNull = ValidationUtil.validateConsultationFee(null);
        assertNotNull("Null fee should be invalid", errNull);
        assertTrue(errNull.contains("required"));

        // Invalid: exceeding NUMBER(8,2) max (999999.99)
        String errTooHigh = ValidationUtil.validateConsultationFee("1000000");
        assertNotNull("Fee exceeding max 999999.99 should be invalid", errTooHigh);
        assertTrue(errTooHigh.contains("cannot exceed"));

        // Numeric double checks
        assertTrue(ValidationUtil.isValidConsultationFee(650.0));
        assertTrue(ValidationUtil.isValidConsultationFee(650.50));
        assertTrue(ValidationUtil.isValidConsultationFee(1000.0));
        assertFalse(ValidationUtil.isValidConsultationFee(0.0));
        assertFalse(ValidationUtil.isValidConsultationFee(-5.0));
        assertFalse(ValidationUtil.isValidConsultationFee(1000000.0));
    }

    @Test
    public void testDoctorFeeValidation() {
        // Fee 0
        String errZero = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "5", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "10:00-12:00", "0");
        assertNotNull(errZero);
        assertTrue(errZero.contains("greater than 0"));

        // Fee negative
        String errNeg = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "5", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "10:00-12:00", "-100");
        assertNotNull(errNeg);
        assertTrue(errNeg.contains("greater than 0"));

        // Fee empty
        String errEmpty = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "5", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "10:00-12:00", "");
        assertNotNull(errEmpty);
        assertTrue(errEmpty.contains("required"));

        // Fee non-numeric
        String errAbc = ValidationUtil.validateDoctorProfile(
                "Dr. Test", "General", "MBBS", "5", "9876543210",
                "test@med.com", "Wing 1", "Mon,Fri", "10:00-12:00", "abc");
        assertNotNull(errAbc);
        assertTrue(errAbc.contains("valid numeric amount"));

        // Positive fee
        assertTrue(ValidationUtil.isValidConsultationFee(500.0));
        assertTrue(ValidationUtil.isValidConsultationFee(650.0));
        assertTrue(ValidationUtil.isValidConsultationFee(650.50));
    }

    // =========================================================================
    // Password Change Validation Tests
    // =========================================================================

    @Test
    public void testPasswordChangeSuccess() {
        String currentPlain = "admin123";
        String storedHash = PasswordUtil.hashPassword(currentPlain);
        String newPlain = "newAdminPass456";
        String confirmPlain = "newAdminPass456";

        String err = ValidationUtil.validatePasswordChange(currentPlain, storedHash, newPlain, confirmPlain);
        assertNull("Valid password change must return null", err);
    }

    @Test
    public void testPasswordChangeWrongCurrentPassword() {
        String storedHash = PasswordUtil.hashPassword("correctPass123");
        String err = ValidationUtil.validatePasswordChange("wrongPass", storedHash, "newSecretPass", "newSecretPass");
        assertNotNull(err);
        assertTrue(err.contains("Incorrect current password"));
    }

    @Test
    public void testPasswordChangeTooShort() {
        String currentPlain = "pass123";
        String storedHash = PasswordUtil.hashPassword(currentPlain);
        String err = ValidationUtil.validatePasswordChange(currentPlain, storedHash, "12345", "12345");
        assertNotNull(err);
        assertTrue(err.contains("at least 8 characters"));
    }

    @Test
    public void testPasswordChangeSameAsCurrent() {
        String currentPlain = "mySecurePassword123";
        String storedHash = PasswordUtil.hashPassword(currentPlain);
        String err = ValidationUtil.validatePasswordChange(currentPlain, storedHash, currentPlain, currentPlain);
        assertNotNull(err);
        assertTrue(err.contains("cannot be the same"));
    }

    @Test
    public void testPasswordChangeConfirmMismatch() {
        String currentPlain = "pass123";
        String storedHash = PasswordUtil.hashPassword(currentPlain);
        String err = ValidationUtil.validatePasswordChange(currentPlain, storedHash, "newPass789", "different789");
        assertNotNull(err);
        assertTrue(err.contains("do not match"));
    }
}

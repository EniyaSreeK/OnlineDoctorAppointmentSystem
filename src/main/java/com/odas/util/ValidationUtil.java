package com.odas.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ValidationUtil centralizes input validation rules across ODAS.
 * 
 * Purpose (for viva):
 * - Defends against invalid or malformed data before reaching the DAO/database layers.
 * - Enforces business rules:
 *     - Patient: Name not empty, Age 1-120, Gender from fixed set, Phone 10 digits, Email format.
 *     - Doctor: Experience 0-60, Available time "HH:mm-HH:mm" with start < end, Available days (Mon-Sun), Fee > 0.
 *     - Password change: Current password verification, min 6 chars, new != current, confirm match.
 */
public class ValidationUtil {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)-([01]\\d|2[0-3]):([0-5]\\d)$");

    private static final Set<String> ALLOWED_GENDERS = new HashSet<>(Arrays.asList("MALE", "FEMALE", "OTHER"));
    private static final Set<String> ALLOWED_DAYS = new HashSet<>(Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // =========================================================================
    // Patient Validations
    // =========================================================================

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public static boolean isValidAge(int age) {
        return age >= 1 && age <= 120;
    }

    public static boolean isValidGender(String gender) {
        return gender != null && ALLOWED_GENDERS.contains(gender.trim().toUpperCase());
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates all patient profile fields.
     * 
     * @return Error message if invalid, or null if all fields are valid.
     */
    public static String validatePatientProfile(String name, String ageStr, String gender, 
                                                String phone, String email, String address) {
        if (!isValidName(name)) {
            return "Patient full name is required.";
        }

        if (ageStr == null || ageStr.trim().isEmpty()) {
            return "Age is required.";
        }
        try {
            int age = Integer.parseInt(ageStr.trim());
            if (!isValidAge(age)) {
                return "Age must be between 1 and 120.";
            }
        } catch (NumberFormatException e) {
            return "Age must be a valid whole number.";
        }

        if (!isValidGender(gender)) {
            return "Please select a valid gender (Male, Female, or Other).";
        }

        if (!isValidPhone(phone)) {
            return "Phone number must be exactly 10 digits.";
        }

        if (!isValidEmail(email)) {
            return "Please enter a valid email address (e.g. user@example.com).";
        }

        return null;
    }

    // =========================================================================
    // Doctor Validations
    // =========================================================================

    public static boolean isValidExperience(int experience) {
        return experience >= 0 && experience <= 60;
    }

    public static final double MAX_CONSULTATION_FEE = 999999.99;

    public static boolean isValidConsultationFee(double fee) {
        return fee > 0 && fee <= MAX_CONSULTATION_FEE;
    }

    /**
     * Validates consultation fee string.
     * Accepts valid numbers like 650, 650.50, and 1000.
     * Rejects 0, negative numbers, empty/null values, non-numeric input, and values > 999999.99 (DB limit NUMBER(8,2)).
     * 
     * @param feeStr the fee input string
     * @return null if valid, or a friendly error message if invalid
     */
    public static String validateConsultationFee(String feeStr) {
        if (feeStr == null || feeStr.trim().isEmpty()) {
            return "Consultation fee is required.";
        }
        try {
            double fee = Double.parseDouble(feeStr.trim());
            if (fee <= 0) {
                return "Consultation fee must be greater than 0.";
            }
            if (fee > MAX_CONSULTATION_FEE) {
                return "Consultation fee cannot exceed 999,999.99.";
            }
            return null;
        } catch (NumberFormatException e) {
            return "Consultation fee must be a valid numeric amount.";
        }
    }

    /**
     * Validates available time range in "HH:mm-HH:mm" format with start before end.
     */
    public static boolean isValidAvailableTime(String timeRange) {
        if (timeRange == null || !TIME_RANGE_PATTERN.matcher(timeRange.trim()).matches()) {
            return false;
        }
        try {
            String[] parts = timeRange.trim().split("-");
            LocalTime start = LocalTime.parse(parts[0], TIME_FORMATTER);
            LocalTime end = LocalTime.parse(parts[1], TIME_FORMATTER);
            return start.isBefore(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates that available days string consists of valid day abbreviations (Mon-Sun).
     * Accepts comma-separated list like "Mon,Wed,Fri" or single day "Mon".
     */
    public static boolean isValidAvailableDays(String daysStr) {
        if (daysStr == null || daysStr.trim().isEmpty()) {
            return false;
        }
        String[] days = daysStr.split(",");
        for (String d : days) {
            String day = d.trim().toUpperCase();
            if (!ALLOWED_DAYS.contains(day)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates all doctor fields for administrative edit/creation.
     * 
     * @return Error message if invalid, or null if all fields are valid.
     */
    public static String validateDoctorProfile(String name, String specialization, String qualification,
                                               String expStr, String phone, String email, String location,
                                               String availableDays, String availableTime, String feeStr) {
        if (!isValidName(name)) {
            return "Doctor name is required.";
        }
        if (specialization == null || specialization.trim().isEmpty()) {
            return "Specialization is required.";
        }

        if (expStr != null && !expStr.trim().isEmpty()) {
            try {
                int exp = Integer.parseInt(expStr.trim());
                if (!isValidExperience(exp)) {
                    return "Experience must be between 0 and 60 years.";
                }
            } catch (NumberFormatException e) {
                return "Experience must be a valid whole number.";
            }
        }

        if (phone != null && !phone.trim().isEmpty() && !isValidPhone(phone)) {
            return "Phone number must be exactly 10 digits.";
        }

        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            return "Please enter a valid email address.";
        }

        if (availableDays != null && !availableDays.trim().isEmpty() && !isValidAvailableDays(availableDays)) {
            return "Available days must be comma-separated days from Mon-Sun (e.g. 'Mon,Wed,Fri').";
        }

        if (availableTime != null && !availableTime.trim().isEmpty() && !isValidAvailableTime(availableTime)) {
            return "Available time must be in 24-hr 'HH:mm-HH:mm' format with start before end (e.g. '09:00-13:00').";
        }

        String feeError = validateConsultationFee(feeStr);
        if (feeError != null) {
            return feeError;
        }

        return null;
    }

    // =========================================================================
    // Password Change Validations
    // =========================================================================

    /**
     * Validates password change parameters.
     * 
     * @param currentPassword plain text current password entered by user
     * @param storedHashedPassword SHA-256 hash stored in database
     * @param newPassword new password
     * @param confirmPassword confirmation password
     * @return Error message if invalid, or null if valid
     */
    public static String validatePasswordChange(String currentPassword, String storedHashedPassword,
                                                String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return "Current password is required.";
        }

        if (!PasswordUtil.checkPassword(currentPassword, storedHashedPassword)) {
            return "Incorrect current password.";
        }

        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return "New password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }

        if (newPassword.equals(currentPassword)) {
            return "New password cannot be the same as your current password.";
        }

        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            return "New password and confirm password do not match.";
        }

        return null;
    }
}

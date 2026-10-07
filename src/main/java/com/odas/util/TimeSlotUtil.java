package com.odas.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TimeSlotUtil manages appointment time slot generation, formatting, and 24-hour reminder calculations.
 * 
 * Purpose (for viva):
 * - Partitions a doctor's AVAILABLE_TIME (e.g. '09:00-13:00') into standard 30-minute consultation slots.
 * - Formats 24-hour time values (e.g. '14:30') into human-readable AM/PM representations (e.g. '02:30 PM').
 * - Calculates whether a scheduled appointment falls within the next 24 hours for proactive patient reminder banners.
 */
public class TimeSlotUtil {

    private static final DateTimeFormatter TIME_24_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_12_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    /**
     * Generates a list of 24-hour 'HH:mm' time slots (e.g. '09:00', '09:30')
     * given an available time range in 'HH:mm-HH:mm' format and slot interval in minutes.
     * 
     * @param availableTimeRange e.g. '09:00-13:00'
     * @param intervalMinutes e.g. 30
     * @return list of slot strings
     */
    public static List<String> generateTimeSlots(String availableTimeRange, int intervalMinutes) {
        List<String> slots = new ArrayList<>();
        if (intervalMinutes <= 0) {
            intervalMinutes = 30;
        }

        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        if (availableTimeRange != null && availableTimeRange.contains("-")) {
            try {
                String[] parts = availableTimeRange.trim().split("-");
                if (parts.length == 2) {
                    LocalTime parsedStart = LocalTime.parse(parts[0].trim(), TIME_24_FORMATTER);
                    LocalTime parsedEnd = LocalTime.parse(parts[1].trim(), TIME_24_FORMATTER);
                    if (parsedStart.isBefore(parsedEnd)) {
                        start = parsedStart;
                        end = parsedEnd;
                    }
                }
            } catch (DateTimeParseException ignored) {
                // Fall back to default 09:00-17:00
            }
        }

        LocalTime current = start;
        while (!current.isAfter(end.minusMinutes(intervalMinutes))) {
            slots.add(current.format(TIME_24_FORMATTER));
            current = current.plusMinutes(intervalMinutes);
        }

        if (slots.isEmpty()) {
            slots.add(start.format(TIME_24_FORMATTER));
        }

        return slots;
    }

    /**
     * Formats a 24-hour time ('14:30') into an AM/PM display label ('02:30 PM').
     */
    public static String formatSlotLabel(String time24) {
        if (time24 == null || time24.trim().isEmpty()) {
            return "";
        }
        try {
            LocalTime t = LocalTime.parse(time24.trim(), TIME_24_FORMATTER);
            return t.format(TIME_12_FORMATTER);
        } catch (DateTimeParseException e) {
            return time24;
        }
    }

    /**
     * Checks if a scheduled appointment date and time falls within the next 24 hours
     * from the current system moment (now to now + 24h).
     */
    public static boolean isWithinNext24Hours(Timestamp appointmentDate, String appointmentTime) {
        return isWithinNext24Hours(appointmentDate, appointmentTime, LocalDateTime.now());
    }

    /**
     * Parses an appointment time slot string (24h "HH:mm" or 12h "hh:mm a") into LocalTime.
     * Defaults to 10:00 on null, empty, or unparseable inputs.
     *
     * @param appointmentTime time string, e.g. "14:30" or "02:30 PM"
     * @return parsed LocalTime
     */
    public static LocalTime parseTime(String appointmentTime) {
        if (appointmentTime == null || appointmentTime.trim().isEmpty()) {
            return LocalTime.of(10, 0);
        }
        String t = appointmentTime.trim();
        try {
            if (t.toUpperCase().endsWith("AM") || t.toUpperCase().endsWith("PM")) {
                return LocalTime.parse(t, TIME_12_FORMATTER);
            } else {
                return LocalTime.parse(t, TIME_24_FORMATTER);
            }
        } catch (DateTimeParseException ignored) {
            return LocalTime.of(10, 0);
        }
    }

    /**
     * Combines an appointment date Timestamp with a time slot string into a LocalDateTime.
     *
     * @param appointmentDate Timestamp representing the date
     * @param appointmentTime string representing the time slot (24h or 12h)
     * @return combined LocalDateTime, or null if appointmentDate is null
     */
    public static LocalDateTime toLocalDateTime(Timestamp appointmentDate, String appointmentTime) {
        if (appointmentDate == null) {
            return null;
        }
        LocalDate date = appointmentDate.toLocalDateTime().toLocalDate();
        LocalTime time = parseTime(appointmentTime);
        return date.atTime(time);
    }

    /**
     * Parameterized overload for testing with an arbitrary reference point.
     */
    public static boolean isWithinNext24Hours(Timestamp appointmentDate, String appointmentTime, LocalDateTime referenceTime) {
        if (appointmentDate == null || referenceTime == null) {
            return false;
        }
        LocalDateTime apptDateTime = toLocalDateTime(appointmentDate, appointmentTime);
        LocalDateTime windowEnd = referenceTime.plusHours(24);

        // Within next 24 hours: must be in future (or right now) and not after 24h from now
        return !apptDateTime.isBefore(referenceTime) && !apptDateTime.isAfter(windowEnd);
    }
}

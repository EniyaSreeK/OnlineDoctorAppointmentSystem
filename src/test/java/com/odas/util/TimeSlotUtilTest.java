package com.odas.util;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for TimeSlotUtil slot generation, label formatting, and 24-hour reminder logic.
 */
public class TimeSlotUtilTest {

    @Test
    public void testGenerateTimeSlotsStandardRanges() {
        // "09:00-11:00" with 30-min intervals should yield 4 slots: 09:00, 09:30, 10:00, 10:30
        List<String> slots = TimeSlotUtil.generateTimeSlots("09:00-11:00", 30);
        assertNotNull(slots);
        assertEquals(4, slots.size());
        assertEquals("09:00", slots.get(0));
        assertEquals("09:30", slots.get(1));
        assertEquals("10:00", slots.get(2));
        assertEquals("10:30", slots.get(3));

        // Afternoon range "14:00-16:00"
        List<String> afternoon = TimeSlotUtil.generateTimeSlots("14:00-16:00", 30);
        assertEquals(4, afternoon.size());
        assertEquals("14:00", afternoon.get(0));
        assertEquals("14:30", afternoon.get(1));
        assertEquals("15:00", afternoon.get(2));
        assertEquals("15:30", afternoon.get(3));
    }

    @Test
    public void testGenerateTimeSlotsEdgeCasesAndFallbacks() {
        // Null or empty range defaults to 09:00-17:00
        List<String> defaultSlots = TimeSlotUtil.generateTimeSlots(null, 30);
        assertNotNull(defaultSlots);
        assertFalse(defaultSlots.isEmpty());
        assertTrue(defaultSlots.contains("09:00"));
        assertTrue(defaultSlots.contains("16:30"));

        List<String> emptySlots = TimeSlotUtil.generateTimeSlots("   ", 30);
        assertNotNull(emptySlots);
        assertEquals(defaultSlots.size(), emptySlots.size());

        // Invalid format falls back
        List<String> malformedSlots = TimeSlotUtil.generateTimeSlots("invalid-range", 30);
        assertNotNull(malformedSlots);
        assertEquals(defaultSlots.size(), malformedSlots.size());

        // Start >= End falls back
        List<String> reversedSlots = TimeSlotUtil.generateTimeSlots("17:00-09:00", 30);
        assertNotNull(reversedSlots);
        assertEquals(defaultSlots.size(), reversedSlots.size());
    }

    @Test
    public void testFormatSlotLabel() {
        assertEquals("09:00 AM", TimeSlotUtil.formatSlotLabel("09:00"));
        assertEquals("09:30 AM", TimeSlotUtil.formatSlotLabel("09:30"));
        assertEquals("12:00 PM", TimeSlotUtil.formatSlotLabel("12:00"));
        assertEquals("12:30 PM", TimeSlotUtil.formatSlotLabel("12:30"));
        assertEquals("01:00 PM", TimeSlotUtil.formatSlotLabel("13:00"));
        assertEquals("02:30 PM", TimeSlotUtil.formatSlotLabel("14:30"));
        assertEquals("06:00 PM", TimeSlotUtil.formatSlotLabel("18:00"));
        assertEquals("11:59 PM", TimeSlotUtil.formatSlotLabel("23:59"));

        // Fallbacks for null / empty / malformed
        assertEquals("", TimeSlotUtil.formatSlotLabel(null));
        assertEquals("", TimeSlotUtil.formatSlotLabel(""));
        assertEquals("invalid", TimeSlotUtil.formatSlotLabel("invalid"));
    }

    @Test
    public void testIsWithinNext24Hours() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 15, 10, 0);

        // Appointment today at 2:00 PM (4 hours later) -> within 24h
        Timestamp apptToday = Timestamp.valueOf(LocalDateTime.of(2026, 10, 15, 0, 0));
        assertTrue(TimeSlotUtil.isWithinNext24Hours(apptToday, "14:00", baseTime));

        // Appointment tomorrow at 9:00 AM (23 hours later) -> within 24h
        Timestamp apptTomorrow = Timestamp.valueOf(LocalDateTime.of(2026, 10, 16, 0, 0));
        assertTrue(TimeSlotUtil.isWithinNext24Hours(apptTomorrow, "09:00", baseTime));

        // Appointment tomorrow at 11:00 AM (25 hours later) -> NOT within 24h
        assertFalse(TimeSlotUtil.isWithinNext24Hours(apptTomorrow, "11:00", baseTime));

        // Appointment earlier today at 8:00 AM -> past, NOT within next 24h
        assertFalse(TimeSlotUtil.isWithinNext24Hours(apptToday, "08:00", baseTime));

        // Appointment yesterday -> past
        Timestamp apptYesterday = Timestamp.valueOf(LocalDateTime.of(2026, 10, 14, 0, 0));
        assertFalse(TimeSlotUtil.isWithinNext24Hours(apptYesterday, "10:00", baseTime));
    }
}

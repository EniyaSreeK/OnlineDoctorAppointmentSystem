package com.odas.appointment;

import com.odas.Appointment;
import com.odas.dao.AppointmentDAO;
import com.odas.util.TimeSlotUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit & Integration tests for STAGE 4 — Time-Slot Booking, Rescheduling & In-App 24h Reminder.
 * 
 * Purpose (for viva):
 * - Validates 30-minute interval slot generation from doctor's AVAILABLE_TIME.
 * - Verifies conflict detection across doctor + date + time slot.
 * - Confirms same-day different-time slot coexistence.
 * - Verifies in-place rescheduling without duplicate row creation and prevents self-collision.
 * - Validates 24-hour reminder calculation for FR7 simulation.
 */
public class Stage4TimeSlotAndRescheduleTest {

    private AppointmentDAO appointmentDAO;
    private List<Integer> createdAppointmentIds;

    @Before
    public void setUp() {
        this.appointmentDAO = new AppointmentDAO();
        this.createdAppointmentIds = new ArrayList<>();
    }

    @After
    public void tearDown() {
        if (!com.odas.util.DBConnection.isAvailable()) {
            return;
        }
        // Clean up test appointments created during the test
        for (Integer id : createdAppointmentIds) {
            try {
                appointmentDAO.deleteAppointment(id);
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void testTimeSlotGeneration() {
        // 09:00 to 13:00 in 30-min intervals => 8 slots (09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30)
        List<String> morningSlots = TimeSlotUtil.generateTimeSlots("09:00-13:00", 30);
        assertEquals(8, morningSlots.size());
        assertEquals("09:00", morningSlots.get(0));
        assertEquals("09:30", morningSlots.get(1));
        assertEquals("12:30", morningSlots.get(7));

        // Afternoon 14:00 to 18:00 => 8 slots
        List<String> afternoonSlots = TimeSlotUtil.generateTimeSlots("14:00-18:00", 30);
        assertEquals(8, afternoonSlots.size());
        assertEquals("14:00", afternoonSlots.get(0));
        assertEquals("17:30", afternoonSlots.get(7));

        // Fallback for null or invalid range
        List<String> defaultSlots = TimeSlotUtil.generateTimeSlots(null, 30);
        assertFalse(defaultSlots.isEmpty());
        assertTrue(defaultSlots.contains("09:00"));
        assertTrue(defaultSlots.contains("16:30"));
    }

    @Test
    public void testFormatSlotLabel() {
        assertEquals("09:00 AM", TimeSlotUtil.formatSlotLabel("09:00"));
        assertEquals("09:30 AM", TimeSlotUtil.formatSlotLabel("09:30"));
        assertEquals("12:00 PM", TimeSlotUtil.formatSlotLabel("12:00"));
        assertEquals("02:30 PM", TimeSlotUtil.formatSlotLabel("14:30"));
        assertEquals("06:00 PM", TimeSlotUtil.formatSlotLabel("18:00"));
    }

    @Test
    public void test24HourReminderDetection() {
        // Reference baseline: Oct 10, 2026 at 10:00 AM
        LocalDateTime baseline = LocalDateTime.of(2026, 10, 10, 10, 0);

        // Case 1: Same day at 2:00 PM (4 hours later) => within 24h
        Timestamp sameDayLater = Timestamp.valueOf(LocalDateTime.of(2026, 10, 10, 0, 0));
        assertTrue(TimeSlotUtil.isWithinNext24Hours(sameDayLater, "14:00", baseline));

        // Case 2: Next day at 9:00 AM (23 hours later) => within 24h
        Timestamp nextDayMorning = Timestamp.valueOf(LocalDateTime.of(2026, 10, 11, 0, 0));
        assertTrue(TimeSlotUtil.isWithinNext24Hours(nextDayMorning, "09:00", baseline));

        // Case 3: Next day at 11:00 AM (25 hours later) => NOT within 24h
        assertFalse(TimeSlotUtil.isWithinNext24Hours(nextDayMorning, "11:00", baseline));

        // Case 4: In the past (same day at 8:00 AM, 2 hours earlier) => NOT in future
        assertFalse(TimeSlotUtil.isWithinNext24Hours(sameDayLater, "08:00", baseline));

        // Case 5: 3 days in the future => NOT within 24h
        Timestamp farFuture = Timestamp.valueOf(LocalDateTime.of(2026, 10, 15, 0, 0));
        assertFalse(TimeSlotUtil.isWithinNext24Hours(farFuture, "10:00", baseline));
    }

    @Test
    public void testBookingAndReschedulingLifecycleWithSlots() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        int doctorId = 1; // Dr. Kumar
        int patientId = 1; // Sample patient
        LocalDate testDate = LocalDate.now().plusDays(40);
        Timestamp dateTs = Timestamp.valueOf(testDate.atStartOfDay());

        String slot1 = "09:30";
        String slot2 = "10:30";
        String slot3 = "11:30";

        // Clean up any stale slot from prior tests
        if (appointmentDAO.isSlotBooked(doctorId, dateTs, slot1)) {
            // Delete if present
        }

        // 1. Initial Booking at 09:30
        Appointment appt1 = new Appointment(doctorId, patientId, dateTs, "SCHEDULED", slot1, "UNPAID");
        int id1 = appointmentDAO.bookAppointment(appt1);
        assertTrue("Initial booking should succeed", id1 > 0);
        createdAppointmentIds.add(id1);

        // 2. Conflict check: 09:30 on same date is now occupied
        assertTrue("Slot 09:30 must be reported as booked", appointmentDAO.isSlotBooked(doctorId, dateTs, slot1));

        // 3. Different slot (10:30) on same doctor and same date must be AVAILABLE
        assertFalse("Slot 10:30 on same day must be free", appointmentDAO.isSlotBooked(doctorId, dateTs, slot2));
        Appointment appt2 = new Appointment(doctorId, patientId, dateTs, "SCHEDULED", slot2, "UNPAID");
        int id2 = appointmentDAO.bookAppointment(appt2);
        assertTrue("Booking different slot on same date must succeed", id2 > 0);
        createdAppointmentIds.add(id2);

        // 4. In-place Reschedule of appt1 from 09:30 to 11:30
        assertFalse("Target slot 11:30 is free", appointmentDAO.isSlotBooked(doctorId, dateTs, slot3));
        assertFalse("isSlotBookedExcept should confirm slot is free",
                appointmentDAO.isSlotBookedExcept(doctorId, dateTs, slot3, id1));

        boolean reschedOk = appointmentDAO.rescheduleAppointment(id1, dateTs, slot3);
        assertTrue("Reschedule appointment in-place must succeed", reschedOk);

        // Verify updated record
        Appointment updatedAppt1 = appointmentDAO.getAppointmentById(id1);
        assertNotNull(updatedAppt1);
        assertEquals("Appointment time must be updated to 11:30", slot3, updatedAppt1.getAppointmentTime());

        // Verify previous slot (09:30) is now freed up
        assertFalse("Previous slot 09:30 must now be free after reschedule",
                appointmentDAO.isSlotBooked(doctorId, dateTs, slot1));

        // 5. Attempting to reschedule appt1 to slot2 (which is occupied by appt2) must be detected as conflict
        assertTrue("Slot occupied by appt2 must trigger conflict for appt1",
                appointmentDAO.isSlotBookedExcept(doctorId, dateTs, slot2, id1));

        // 6. Test patient conflict check (SRS Section 8 Constraint: Patients cannot book multiple appointments for same slot)
        assertTrue("Patient slot conflict check must detect patient is booked for slot 10:30 (appt2)",
                appointmentDAO.isPatientSlotBooked(patientId, dateTs, slot2));
        assertFalse("Patient slot conflict check must report slot 09:30 as free",
                appointmentDAO.isPatientSlotBooked(patientId, dateTs, slot1));
        assertTrue("isPatientSlotBookedExcept must detect conflict for appt1 on slot2",
                appointmentDAO.isPatientSlotBookedExcept(patientId, dateTs, slot2, id1));
        assertFalse("isPatientSlotBookedExcept must ignore own appointment id1 when checking slot3",
                appointmentDAO.isPatientSlotBookedExcept(patientId, dateTs, slot3, id1));
    }
}

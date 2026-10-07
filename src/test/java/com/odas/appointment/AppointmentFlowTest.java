package com.odas.appointment;

import com.odas.Appointment;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Unit tests for Stage 4 Appointment lifecycle and validation rules.
 */
public class AppointmentFlowTest {

    @Test
    public void testDateValidationRules() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(7);
        LocalDate pastDate = today.minusDays(1);

        assertFalse("Today should not be considered past", today.isBefore(LocalDate.now()));
        assertFalse("Future date should not be before today", futureDate.isBefore(LocalDate.now()));
        assertTrue("Past date must be detected as before today", pastDate.isBefore(LocalDate.now()));
    }

    @Test
    public void testAppointmentStatusLifecycle() {
        Timestamp apptTime = Timestamp.valueOf(LocalDateTime.now().plusDays(2));
        Appointment appt = new Appointment(1, 10, 20, apptTime, "SCHEDULED");

        assertEquals("SCHEDULED", appt.getStatus());

        // Cancel status transition
        appt.setStatus("CANCELLED");
        assertEquals("CANCELLED", appt.getStatus());

        // Completed status transition
        appt.setStatus("COMPLETED");
        assertEquals("COMPLETED", appt.getStatus());
    }

    @Test
    public void testAppointmentDisplayFields() {
        Timestamp apptTime = Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        Appointment appt = new Appointment(5, 1, 2, apptTime, "SCHEDULED");
        appt.setDoctorName("Dr. Kumar");
        appt.setDoctorSpecialization("Cardiologist");
        appt.setPatientName("Eniya Sree K");

        assertEquals("Dr. Kumar", appt.getDoctorName());
        assertEquals("Cardiologist", appt.getDoctorSpecialization());
        assertEquals("Eniya Sree K", appt.getPatientName());
        assertEquals(5, appt.getId());
    }

    @Test
    public void testUpcomingVsPastDetermination() {
        LocalDate today = LocalDate.now();
        // Today midnight appointment (was previously miscategorized by Timestamp.after(now))
        Timestamp todayMidnight = Timestamp.valueOf(today.atStartOfDay());
        LocalDate apptDateToday = todayMidnight.toLocalDateTime().toLocalDate();
        assertFalse("Appointment for today must not be marked as past", apptDateToday.isBefore(today));

        // Future appointment
        Timestamp futureAppt = Timestamp.valueOf(today.plusDays(3).atStartOfDay());
        LocalDate apptDateFuture = futureAppt.toLocalDateTime().toLocalDate();
        assertFalse("Future appointment must not be marked as past", apptDateFuture.isBefore(today));

        // Yesterday appointment
        Timestamp yesterdayAppt = Timestamp.valueOf(today.minusDays(1).atStartOfDay());
        LocalDate apptDateYesterday = yesterdayAppt.toLocalDateTime().toLocalDate();
        assertTrue("Past appointment must be detected as before today", apptDateYesterday.isBefore(today));
    }
}

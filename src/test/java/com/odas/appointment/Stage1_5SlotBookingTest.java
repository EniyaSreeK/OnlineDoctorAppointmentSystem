package com.odas.appointment;

import com.odas.Appointment;
import com.odas.dao.AppointmentDAO;
import com.odas.util.DBConnection;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * Unit & Integration test for STAGE 1.5 — Appointment slot rebooking & double-booking prevention.
 * 
 * Verifies requirement:
 * "Add a unit test: book, cancel, rebook the same slot succeeds; booking an active slot twice fails."
 */
public class Stage1_5SlotBookingTest {

    @BeforeClass
    public static void setUpClass() {
        Assume.assumeTrue("Oracle DB is reachable", DBConnection.isAvailable());
    }

    @Test
    public void testBookCancelAndRebookSlotCycle() {
        AppointmentDAO appointmentDAO = new AppointmentDAO();

        int doctorId = 1;
        int patientId = 1;
        // Far future date to guarantee no collision with real testing data
        LocalDate testDate = LocalDate.now().plusDays(365);
        Timestamp appointmentTimestamp = Timestamp.valueOf(testDate.atStartOfDay());
        String testSlot = "15:30";

        // Step 0: Ensure slot is initially free
        assertFalse("Slot should initially be free", 
                appointmentDAO.isSlotBooked(doctorId, appointmentTimestamp, testSlot));

        // Step 1: Book first appointment for this slot
        Appointment appt1 = new Appointment();
        appt1.setDoctorId(doctorId);
        appt1.setPatientId(patientId);
        appt1.setAppointmentDate(appointmentTimestamp);
        appt1.setAppointmentTime(testSlot);
        appt1.setStatus("SCHEDULED");
        appt1.setPaymentStatus("UNPAID");

        int appt1Id = appointmentDAO.bookAppointment(appt1);
        assertTrue("Initial booking should succeed", appt1Id > 0);

        try {
            // Step 2: Slot is now marked as booked for active appointments
            assertTrue("Slot should be booked now", 
                    appointmentDAO.isSlotBooked(doctorId, appointmentTimestamp, testSlot));

            // Step 3: Booking an active slot twice must fail
            Appointment apptDuplicate = new Appointment();
            apptDuplicate.setDoctorId(doctorId);
            apptDuplicate.setPatientId(patientId);
            apptDuplicate.setAppointmentDate(appointmentTimestamp);
            apptDuplicate.setAppointmentTime(testSlot);
            apptDuplicate.setStatus("SCHEDULED");
            apptDuplicate.setPaymentStatus("UNPAID");

            int duplicateId = appointmentDAO.bookAppointment(apptDuplicate);
            assertTrue("Booking an already active slot must fail (negative code)", duplicateId <= 0);

            // Step 4: Cancel the first appointment
            boolean cancelled = appointmentDAO.cancelAppointment(appt1Id);
            assertTrue("Appointment cancellation must succeed", cancelled);

            // Step 5: After cancellation, slot must be available for rebooking
            assertFalse("Cancelled appointment slot must be free for rebooking",
                    appointmentDAO.isSlotBooked(doctorId, appointmentTimestamp, testSlot));

            // Step 6: Rebooking the cancelled slot should now SUCCEED
            Appointment apptRebook = new Appointment();
            apptRebook.setDoctorId(doctorId);
            apptRebook.setPatientId(patientId);
            apptRebook.setAppointmentDate(appointmentTimestamp);
            apptRebook.setAppointmentTime(testSlot);
            apptRebook.setStatus("SCHEDULED");
            apptRebook.setPaymentStatus("UNPAID");

            int rebookId = appointmentDAO.bookAppointment(apptRebook);
            assertTrue("Rebooking the same previously cancelled slot must succeed", rebookId > 0);

            try {
                // Step 7: Slot is once again active
                assertTrue("Rebooked slot must be booked", 
                        appointmentDAO.isSlotBooked(doctorId, appointmentTimestamp, testSlot));
            } finally {
                // Cleanup rebooked appointment
                appointmentDAO.deleteAppointment(rebookId);
            }

        } finally {
            // Cleanup initial appointment
            appointmentDAO.deleteAppointment(appt1Id);
        }
    }
}

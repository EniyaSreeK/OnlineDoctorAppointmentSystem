package com.odas.notify;

import com.odas.Appointment;
import com.odas.Patient;
import com.odas.dao.AppointmentDAO;
import com.odas.util.TimeSlotUtil;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for NotificationService and ReminderScheduler (FR5, FR6, FR7).
 * Verifies SMS dispatch, provider failure resilience, and 24-hr reminder window calculation.
 */
public class NotificationServiceTest {

    static class CapturingSmsProvider implements SmsProvider {
        final List<String> capturedPhones = new ArrayList<>();
        final List<String> capturedMessages = new ArrayList<>();
        boolean failWithException = false;
        boolean returnResult = true;

        @Override
        public boolean send(String toPhone, String message) {
            if (failWithException) {
                throw new RuntimeException("Simulated SMS network failure");
            }
            capturedPhones.add(toPhone);
            capturedMessages.add(message);
            return returnResult;
        }
    }

    private Appointment createTestAppointment(String phone, String email, String doctorName, String dateStr, String timeStr) {
        Appointment appt = new Appointment();
        appt.setId(101);
        appt.setDoctorId(1);
        appt.setPatientId(2);
        appt.setDoctorName(doctorName);
        java.time.LocalDate parsed = java.time.LocalDate.parse(dateStr);
        appt.setAppointmentDate(Timestamp.valueOf(parsed.atStartOfDay()));
        appt.setDate(dateStr);
        appt.setAppointmentTime(timeStr);
        appt.setStatus("SCHEDULED");

        Patient patient = new Patient();
        patient.setId(2);
        patient.setName("Jane Doe");
        patient.setPhone(phone);
        patient.setEmail(email);
        appt.setPatient(patient);

        return appt;
    }

    @Test
    public void testBookingConfirmationDispatchesSms() {
        CapturingSmsProvider mockSms = new CapturingSmsProvider();
        EmailSender mockEmail = new EmailSender(); // In absence of SMTP env vars, email is skipped safely
        NotificationService service = new NotificationService(mockSms, mockEmail, null, null);

        Appointment appt = createTestAppointment("9876543210", null, "Dr. Alice Smith", "2026-10-15", "10:30");

        boolean sent = service.sendBookingConfirmation(appt);

        assertTrue("Expected booking confirmation to report success via SMS channel", sent);
        assertEquals(1, mockSms.capturedPhones.size());
        assertEquals("9876543210", mockSms.capturedPhones.get(0));

        String msg = mockSms.capturedMessages.get(0);
        assertTrue("Message should contain doctor name", msg.contains("Dr. Alice Smith") || msg.contains("Alice Smith"));
        assertTrue("Message should contain appointment date", msg.contains("2026-10-15"));
        assertTrue("Message should contain time slot", msg.contains("10:30"));
        assertTrue("Message should contain ODAS prefix", msg.startsWith("ODAS:"));
    }

    @Test
    public void testCancellationConfirmationDispatchesSms() {
        CapturingSmsProvider mockSms = new CapturingSmsProvider();
        NotificationService service = new NotificationService(mockSms, null, null, null);

        Appointment appt = createTestAppointment("9876543210", null, "Dr. Alice Smith", "2026-10-15", "10:30");

        boolean sent = service.sendCancellationConfirmation(appt);

        assertTrue("Expected cancellation notification to succeed", sent);
        assertEquals(1, mockSms.capturedMessages.size());
        String msg = mockSms.capturedMessages.get(0);
        assertTrue("Message should state cancelled", msg.contains("cancelled"));
    }

    @Test
    public void testProviderFailureDoesNotThrow() {
        CapturingSmsProvider failingSms = new CapturingSmsProvider();
        failingSms.failWithException = true;

        NotificationService service = new NotificationService(failingSms, null, null, null);
        Appointment appt = createTestAppointment("9876543210", null, "Dr. Bob", "2026-10-20", "14:00");

        // Booking confirmation should catch exception internally and return false
        boolean bookingResult = false;
        try {
            bookingResult = service.sendBookingConfirmation(appt);
        } catch (Throwable t) {
            fail("sendBookingConfirmation must never throw an uncaught exception: " + t.getMessage());
        }
        assertFalse("Booking notification should report false when SMS provider fails", bookingResult);

        // Cancellation confirmation should catch exception internally and return false
        boolean cancelResult = false;
        try {
            cancelResult = service.sendCancellationConfirmation(appt);
        } catch (Throwable t) {
            fail("sendCancellationConfirmation must never throw an uncaught exception: " + t.getMessage());
        }
        assertFalse("Cancellation notification should report false when SMS provider fails", cancelResult);

        // Reminder should catch exception internally and return false
        boolean reminderResult = false;
        try {
            reminderResult = service.sendReminder(appt);
        } catch (Throwable t) {
            fail("sendReminder must never throw an uncaught exception: " + t.getMessage());
        }
        assertFalse("Reminder notification should report false when SMS provider fails", reminderResult);
    }

    @Test
    public void testUpcomingReminderWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 10, 0, 0);

        // Within 24-hr window
        LocalDateTime in1Hour = now.plusHours(1);
        LocalDateTime in12Hours = now.plusHours(12);
        LocalDateTime in24Hours = now.plusHours(24);

        assertTrue("1 hour in future must be inside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, in1Hour, 24));
        assertTrue("12 hours in future must be inside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, in12Hours, 24));
        assertTrue("Exactly 24 hours in future must be inside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, in24Hours, 24));

        // Outside 24-hr window (past)
        LocalDateTime past1Hour = now.minusHours(1);
        LocalDateTime pastYesterday = now.minusDays(1);

        assertFalse("1 hour in past must be outside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, past1Hour, 24));
        assertFalse("Yesterday must be outside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, pastYesterday, 24));

        // Outside 24-hr window (too far in future)
        LocalDateTime in25Hours = now.plusHours(25);
        LocalDateTime in2Days = now.plusDays(2);

        assertFalse("25 hours in future must be outside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, in25Hours, 24));
        assertFalse("2 days in future must be outside 24h window",
                ReminderScheduler.isWithinReminderWindow(now, in2Days, 24));

        // Null handling
        assertFalse("Null now must return false",
                ReminderScheduler.isWithinReminderWindow(null, in12Hours, 24));
        assertFalse("Null appointment time must return false",
                ReminderScheduler.isWithinReminderWindow(now, null, 24));
    }

    @Test
    public void testMockSmsProviderAlwaysSucceeds() {
        MockSmsProvider mock = new MockSmsProvider();
        boolean result = mock.send("9876543210", "Test message from ODAS");
        assertTrue("MockSmsProvider must return true", result);
    }

    @Test
    public void testFakeClockReminderTimingWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 10, 0, 0);
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        // 1. Appointment today at 18:00 (now = 10:00) IS reminded
        Timestamp apptToday18 = Timestamp.valueOf(today.atStartOfDay());
        LocalDateTime dtToday18 = TimeSlotUtil.toLocalDateTime(apptToday18, "18:00");
        assertTrue("Today at 18:00 (now=10:00) IS reminded",
                ReminderScheduler.isWithinReminderWindow(now, dtToday18, 24));

        // 2. Appointment tomorrow at 09:00 (now = 10:00) IS reminded
        Timestamp apptTom09 = Timestamp.valueOf(tomorrow.atStartOfDay());
        LocalDateTime dtTom09 = TimeSlotUtil.toLocalDateTime(apptTom09, "09:00");
        assertTrue("Tomorrow at 09:00 (now=10:00) IS reminded",
                ReminderScheduler.isWithinReminderWindow(now, dtTom09, 24));

        // 3. Appointment tomorrow at 11:00 (now = 10:00) is NOT reminded (25h away)
        Timestamp apptTom11 = Timestamp.valueOf(tomorrow.atStartOfDay());
        LocalDateTime dtTom11 = TimeSlotUtil.toLocalDateTime(apptTom11, "11:00");
        assertFalse("Tomorrow at 11:00 (now=10:00) is NOT reminded",
                ReminderScheduler.isWithinReminderWindow(now, dtTom11, 24));

        // 4. Appointment today at 08:00 (now = 10:00) is NOT reminded (past)
        Timestamp apptToday08 = Timestamp.valueOf(today.atStartOfDay());
        LocalDateTime dtToday08 = TimeSlotUtil.toLocalDateTime(apptToday08, "08:00");
        assertFalse("Today at 08:00 (now=10:00) is NOT reminded",
                ReminderScheduler.isWithinReminderWindow(now, dtToday08, 24));
    }

    @Test
    public void testReminderSchedulerWithFakeClockAndMissingContacts() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 10, 0, 0);
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        List<Integer> markedIds = new ArrayList<>();
        CapturingSmsProvider mockSms = new CapturingSmsProvider();
        NotificationService service = new NotificationService(mockSms, null, null, null);

        AppointmentDAO stubDao = new AppointmentDAO() {
            @Override
            public List<Appointment> getUpcomingUnreminded(LocalDate refDate) {
                List<Appointment> list = new ArrayList<>();

                // appt 1: today 18:00 (eligible)
                Appointment a1 = createTestAppointment("9876543210", null, "Dr. Alice", today.toString(), "18:00");
                a1.setId(1);
                a1.setAppointmentDate(Timestamp.valueOf(today.atStartOfDay()));
                list.add(a1);

                // appt 2: tomorrow 09:00 (eligible)
                Appointment a2 = createTestAppointment("9876543210", null, "Dr. Bob", tomorrow.toString(), "09:00");
                a2.setId(2);
                a2.setAppointmentDate(Timestamp.valueOf(tomorrow.atStartOfDay()));
                list.add(a2);

                // appt 3: tomorrow 11:00 (outside 24h)
                Appointment a3 = createTestAppointment("9876543210", null, "Dr. Charlie", tomorrow.toString(), "11:00");
                a3.setId(3);
                a3.setAppointmentDate(Timestamp.valueOf(tomorrow.atStartOfDay()));
                list.add(a3);

                // appt 4: today 08:00 (past)
                Appointment a4 = createTestAppointment("9876543210", null, "Dr. David", today.toString(), "08:00");
                a4.setId(4);
                a4.setAppointmentDate(Timestamp.valueOf(today.atStartOfDay()));
                list.add(a4);

                // appt 5: today 16:00, but patient has neither phone nor email
                Appointment a5 = createTestAppointment(null, null, "Dr. Eve", today.toString(), "16:00");
                a5.setId(5);
                a5.setAppointmentDate(Timestamp.valueOf(today.atStartOfDay()));
                list.add(a5);

                return list;
            }

            @Override
            public boolean markReminderSent(int id) {
                markedIds.add(id);
                return true;
            }
        };

        ReminderScheduler scheduler = new ReminderScheduler(stubDao, service);
        int sent = scheduler.processUpcomingReminders(now);

        // appt 1 and appt 2 had notifications dispatched successfully
        assertEquals("Two appointments should have had reminders sent", 2, sent);

        // Appt 1 (today 18:00) and Appt 2 (tomorrow 09:00) were sent
        assertEquals(2, mockSms.capturedMessages.size());
        assertTrue("SMS should contain 18:00 or 06:00 PM",
                mockSms.capturedMessages.get(0).contains("06:00 PM") || mockSms.capturedMessages.get(0).contains("18:00"));
        assertTrue("SMS should contain 09:00 AM or 09:00",
                mockSms.capturedMessages.get(1).contains("09:00 AM") || mockSms.capturedMessages.get(1).contains("09:00"));

        // Appt 1 and 2 were marked sent, Appt 5 was marked sent (no phone/email),
        // Appt 3 (tomorrow 11:00) was NOT marked, Appt 4 (today 08:00) was NOT marked
        assertTrue("Appt 1 must be marked sent", markedIds.contains(1));
        assertTrue("Appt 2 must be marked sent", markedIds.contains(2));
        assertTrue("Appt 5 (no contact info) must be marked sent to prevent infinite retries", markedIds.contains(5));
        assertFalse("Appt 3 (tomorrow 11:00, 25h away) must NOT be marked sent", markedIds.contains(3));
        assertFalse("Appt 4 (today 08:00, past) must NOT be marked sent", markedIds.contains(4));
    }
}

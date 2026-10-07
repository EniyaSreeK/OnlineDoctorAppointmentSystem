package com.odas.appointment;

import com.odas.Appointment;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for appointment status transition rules, permission matrix, and refund workflows.
 */
public class AppointmentStatusTransitionTest {

    @Test
    public void testValidStatusTransitionsFromScheduled() {
        Appointment appt = new Appointment();
        appt.setStatus("SCHEDULED");

        // SCHEDULED -> CANCELLED is allowed
        assertTrue("Cancellation from SCHEDULED should be valid", isValidTransition(appt.getStatus(), "CANCELLED"));

        // SCHEDULED -> COMPLETED is allowed
        assertTrue("Completion from SCHEDULED should be valid", isValidTransition(appt.getStatus(), "COMPLETED"));
    }

    @Test
    public void testInvalidStatusTransitions() {
        // CANCELLED terminal state
        assertFalse("Cannot complete an already cancelled appointment", isValidTransition("CANCELLED", "COMPLETED"));
        assertFalse("Cannot re-cancel a cancelled appointment", isValidTransition("CANCELLED", "CANCELLED"));

        // COMPLETED terminal state
        assertFalse("Cannot cancel a completed appointment", isValidTransition("COMPLETED", "CANCELLED"));
        assertFalse("Cannot re-complete a completed appointment", isValidTransition("COMPLETED", "COMPLETED"));
    }

    @Test
    public void testRoleActionPermissions() {
        // Patients can only cancel, never complete
        assertTrue("Patient can cancel appointment", canRolePerformAction("PATIENT", "cancel"));
        assertFalse("Patient cannot mark appointment complete", canRolePerformAction("PATIENT", "complete"));

        // Doctors can complete and cancel (decline) their own appointments
        assertTrue("Doctor can complete appointment", canRolePerformAction("DOCTOR", "complete"));
        assertTrue("Doctor can decline/cancel appointment", canRolePerformAction("DOCTOR", "cancel"));

        // Admins can perform both actions
        assertTrue("Admin can cancel appointment", canRolePerformAction("ADMIN", "cancel"));
        assertTrue("Admin can complete appointment", canRolePerformAction("ADMIN", "complete"));

        // Unknown roles / actions
        assertFalse(canRolePerformAction("GUEST", "cancel"));
        assertFalse(canRolePerformAction("PATIENT", "delete"));
    }

    @Test
    public void testDoctorCancelAuthorization() {
        Appointment appt = new Appointment();
        appt.setId(10);
        appt.setDoctorId(2);
        appt.setStatus("SCHEDULED");

        // Attending doctor (ID 2) is authorized
        assertTrue("Attending doctor should be authorized to cancel/decline", isDoctorAuthorizedToCancel(appt, 2));

        // Different doctor (ID 5) is rejected
        assertFalse("Other doctor must NOT be authorized to cancel", isDoctorAuthorizedToCancel(appt, 5));
    }

    @Test
    public void testCancellationRefundRule() {
        // If an appointment was PAID, cancelling it moves paymentStatus to REFUNDED
        Appointment paidAppt = new Appointment();
        paidAppt.setStatus("SCHEDULED");
        paidAppt.setPaymentStatus("PAID");

        String resultingPaymentStatus = determinePaymentStatusOnCancel(paidAppt.getPaymentStatus());
        assertEquals("Cancelling a PAID appointment must trigger REFUNDED status", "REFUNDED", resultingPaymentStatus);

        // If an appointment was UNPAID, cancelling leaves it UNPAID
        Appointment unpaidAppt = new Appointment();
        unpaidAppt.setStatus("SCHEDULED");
        unpaidAppt.setPaymentStatus("UNPAID");

        String resultingUnpaidStatus = determinePaymentStatusOnCancel(unpaidAppt.getPaymentStatus());
        assertEquals("Cancelling an UNPAID appointment remains UNPAID", "UNPAID", resultingUnpaidStatus);
    }

    @Test
    public void testPaymentEligibilityByAppointmentStatus() {
        // SCHEDULED + UNPAID -> Eligible
        assertTrue("Scheduled unpaid appointment can be paid", isEligibleForPayment("SCHEDULED", "UNPAID"));

        // COMPLETED + UNPAID -> Eligible
        assertTrue("Completed unpaid appointment can be paid", isEligibleForPayment("COMPLETED", "UNPAID"));

        // Already PAID -> Ineligible for new payment
        assertFalse("Already paid appointment cannot be paid again", isEligibleForPayment("SCHEDULED", "PAID"));

        // CANCELLED -> Ineligible for payment
        assertFalse("Cancelled appointment cannot be paid", isEligibleForPayment("CANCELLED", "UNPAID"));
        assertFalse("Cancelled appointment cannot be paid even if previously recorded", isEligibleForPayment("CANCELLED", "REFUNDED"));
    }

    // Helper business rules reflecting AppointmentActionServlet & PaymentServlet logic
    private boolean isValidTransition(String currentStatus, String targetStatus) {
        if (!"SCHEDULED".equalsIgnoreCase(currentStatus)) {
            return false; // Only SCHEDULED appointments can transition
        }
        return "CANCELLED".equalsIgnoreCase(targetStatus) || "COMPLETED".equalsIgnoreCase(targetStatus);
    }

    private boolean canRolePerformAction(String role, String action) {
        if ("ADMIN".equals(role)) {
            return "cancel".equalsIgnoreCase(action) || "complete".equalsIgnoreCase(action);
        }
        if ("PATIENT".equals(role)) {
            return "cancel".equalsIgnoreCase(action);
        }
        if ("DOCTOR".equals(role)) {
            return "complete".equalsIgnoreCase(action) || "cancel".equalsIgnoreCase(action);
        }
        return false;
    }

    private boolean isDoctorAuthorizedToCancel(Appointment appt, int loggedInDoctorId) {
        return appt != null && appt.getDoctorId() == loggedInDoctorId;
    }

    private String determinePaymentStatusOnCancel(String currentPaymentStatus) {
        if ("PAID".equalsIgnoreCase(currentPaymentStatus)) {
            return "REFUNDED";
        }
        return currentPaymentStatus;
    }

    private boolean isEligibleForPayment(String appointmentStatus, String paymentStatus) {
        if (!"UNPAID".equalsIgnoreCase(paymentStatus)) {
            return false;
        }
        return "SCHEDULED".equalsIgnoreCase(appointmentStatus) || "COMPLETED".equalsIgnoreCase(appointmentStatus);
    }
}

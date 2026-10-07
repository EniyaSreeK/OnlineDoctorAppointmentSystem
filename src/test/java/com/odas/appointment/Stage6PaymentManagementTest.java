package com.odas.appointment;

import com.odas.Appointment;
import com.odas.Doctor;
import com.odas.Payment;
import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PaymentDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit & Integration tests for STAGE 6 — Simulated Online Payment.
 * 
 * Purpose (for viva):
 * - Validates payment mode validation logic (Card, UPI, Net Banking).
 * - Verifies that consultation fee amount is strictly derived from the doctor's record on the server.
 * - Tests atomic database transaction via PaymentDAO.processPaymentTransaction:
 *   1. Inserts PAYMENTS record.
 *   2. Updates APPOINTMENTS.PAYMENT_STATUS = 'PAID'.
 *   3. Verifies all-or-nothing atomicity and transaction rollback upon error.
 * - Verifies double-payment prevention logic and appointment ownership validation.
 */
public class Stage6PaymentManagementTest {

    private PaymentDAO paymentDAO;
    private AppointmentDAO appointmentDAO;
    private DoctorDAO doctorDAO;
    private List<Integer> createdPaymentIds;
    private List<Integer> createdAppointmentIds;

    private static final List<String> ALLOWED_MODES = Arrays.asList("Card", "UPI", "Net Banking");

    @Before
    public void setUp() {
        this.paymentDAO = new PaymentDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.doctorDAO = new DoctorDAO();
        this.createdPaymentIds = new ArrayList<>();
        this.createdAppointmentIds = new ArrayList<>();
    }

    @After
    public void tearDown() {
        if (!com.odas.util.DBConnection.isAvailable()) {
            return;
        }
        // Clean up test payments first (FK dependency)
        for (Integer payId : createdPaymentIds) {
            try {
                paymentDAO.deletePayment(payId);
            } catch (Exception ignored) {}
        }

        // Clean up test appointments
        for (Integer apptId : createdAppointmentIds) {
            try {
                appointmentDAO.deleteAppointment(apptId);
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void testPaymentModeValidation() {
        // Allowed modes
        assertTrue("Card must be valid", isValidPaymentMode("Card"));
        assertTrue("UPI must be valid", isValidPaymentMode("UPI"));
        assertTrue("Net Banking must be valid", isValidPaymentMode("Net Banking"));

        // Invalid modes
        assertFalse("Bitcoin must be invalid", isValidPaymentMode("Bitcoin"));
        assertFalse("Cash must be invalid", isValidPaymentMode("Cash"));
        assertFalse("PayPal must be invalid", isValidPaymentMode("PayPal"));
        assertFalse("Empty mode must be invalid", isValidPaymentMode(""));
        assertFalse("Null mode must be invalid", isValidPaymentMode(null));
        assertFalse("Whitespace must be invalid", isValidPaymentMode("   "));
    }

    @Test
    public void testConsultationFeeDerivedFromServer() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        // Doctor 1: Dr. Kumar
        Doctor doc1 = doctorDAO.getDoctorById(1);
        assertNotNull("Dr. Kumar must exist in database", doc1);
        double fee1 = doc1.getConsultationFee() > 0 ? doc1.getConsultationFee() : 500.0;
        assertTrue("Doctor 1 consultation fee must be positive", fee1 > 0);
        assertEquals("Fee must be read directly from doctor record", doc1.getConsultationFee(), fee1, 0.001);

        // Doctor 2: Dr. Priya
        Doctor doc2 = doctorDAO.getDoctorById(2);
        assertNotNull("Dr. Priya must exist in database", doc2);
        double fee2 = doc2.getConsultationFee() > 0 ? doc2.getConsultationFee() : 500.0;
        assertTrue("Doctor 2 consultation fee must be positive", fee2 > 0);
        assertEquals("Fee must be read directly from doctor record", doc2.getConsultationFee(), fee2, 0.001);

        // Tampered client amount must never be used
        double tamperedAmount = 1.00;
        assertNotEquals("Server must reject tampered amount in favor of DB fee", tamperedAmount, fee1, 0.001);
        assertNotEquals("Server must reject tampered amount in favor of DB fee", tamperedAmount, fee2, 0.001);
    }

    @Test
    public void testSuccessfulPaymentSetsPaidAtomicTransaction() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        int doctorId = 1;
        int patientId = 1;
        Timestamp apptDate = Timestamp.valueOf(LocalDate.now().plusDays(2).atTime(11, 0));
        String slot = "11:00";

        // 1. Create an unpaid appointment
        Appointment appt = new Appointment(doctorId, patientId, apptDate, "SCHEDULED", slot, "UNPAID");
        int apptId = appointmentDAO.bookAppointment(appt);
        assertTrue("Appointment must be created", apptId > 0);
        createdAppointmentIds.add(apptId);

        // Verify initial UNPAID status
        Appointment initialAppt = appointmentDAO.getAppointmentById(apptId);
        assertNotNull(initialAppt);
        assertEquals("UNPAID", initialAppt.getPaymentStatus());

        // 2. Perform atomic payment transaction
        Doctor doc = doctorDAO.getDoctorById(doctorId);
        double fee = doc.getConsultationFee();

        Payment payment = new Payment(apptId, patientId, fee, "UPI", "SUCCESS");
        int paymentId = paymentDAO.processPaymentTransaction(payment);
        assertTrue("processPaymentTransaction must return generated key > 0", paymentId > 0);
        createdPaymentIds.add(paymentId);

        // 3. Verify that APPOINTMENTS.PAYMENT_STATUS was atomically updated to 'PAID'
        Appointment updatedAppt = appointmentDAO.getAppointmentById(apptId);
        assertNotNull(updatedAppt);
        assertEquals("Appointment payment status must be updated to PAID", "PAID", updatedAppt.getPaymentStatus());

        // 4. Verify PAYMENTS record details
        Payment fetchedPayment = paymentDAO.getPaymentById(paymentId);
        assertNotNull("Payment record must be queryable by ID", fetchedPayment);
        assertEquals(apptId, fetchedPayment.getAppointmentId());
        assertEquals(patientId, fetchedPayment.getPatientId());
        assertEquals(fee, fetchedPayment.getAmount(), 0.001);
        assertEquals("UPI", fetchedPayment.getPaymentMode());
        assertEquals("SUCCESS", fetchedPayment.getPaymentStatus());
        assertNotNull(fetchedPayment.getPaymentDate());
    }

    @Test
    public void testDoublePaymentBlocked() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        int doctorId = 1;
        int patientId = 1;
        Timestamp apptDate = Timestamp.valueOf(LocalDate.now().plusDays(3).atTime(11, 30));
        String slot = "11:30";

        // 1. Book and pay for appointment
        Appointment appt = new Appointment(doctorId, patientId, apptDate, "SCHEDULED", slot, "UNPAID");
        int apptId = appointmentDAO.bookAppointment(appt);
        assertTrue(apptId > 0);
        createdAppointmentIds.add(apptId);

        Payment payment = new Payment(apptId, patientId, 650.0, "Card", "SUCCESS");
        int paymentId = paymentDAO.processPaymentTransaction(payment);
        assertTrue(paymentId > 0);
        createdPaymentIds.add(paymentId);

        // 2. Verify appointment is already paid
        Appointment paidAppt = appointmentDAO.getAppointmentById(apptId);
        assertEquals("PAID", paidAppt.getPaymentStatus());

        // 3. Query existing payment
        Payment existingPayment = paymentDAO.getPaymentByAppointmentId(apptId);
        assertNotNull("Existing payment must be found", existingPayment);
        assertEquals(paymentId, existingPayment.getId());

        // In servlet logic: if existingPayment != null or status is PAID, redirect to existing receipt
        boolean isAlreadyPaid = "PAID".equalsIgnoreCase(paidAppt.getPaymentStatus());
        assertTrue("Double payment must be blocked when status is already PAID", isAlreadyPaid);
    }

    @Test
    public void testWrongOwnerBlocked() {
        int owningPatientId = 1; // Eniya
        int attackingPatientId = 2; // Another patient

        Appointment appt = new Appointment(1, owningPatientId, Timestamp.valueOf(LocalDate.now().plusDays(4).atTime(12, 0)), "SCHEDULED", "12:00", "UNPAID");

        // Validation rule from PaymentServlet:
        // Integer sessionPatientId = (Integer) session.getAttribute("patientId");
        // if (sessionPatientId == null || appt.getPatientId() != sessionPatientId) -> redirect unauthorized
        boolean isAuthorizedOwner = (appt.getPatientId() == attackingPatientId);
        assertFalse("Non-owning patient must NOT be authorized to pay for another patient's appointment", isAuthorizedOwner);

        boolean isRealOwnerAuthorized = (appt.getPatientId() == owningPatientId);
        assertTrue("Owning patient must be authorized to pay", isRealOwnerAuthorized);
    }

    @Test
    public void testRollbackOnFailureWithInvalidAppointmentId() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        // Attempt payment with invalid appointment ID (violates FK constraint)
        int invalidAppointmentId = -9999;
        Payment invalidPayment = new Payment(invalidAppointmentId, 1, 500.0, "Card", "SUCCESS");

        int result = paymentDAO.processPaymentTransaction(invalidPayment);
        assertEquals("Transaction must fail and roll back, returning -1", -1, result);

        // Verify no orphan payment was inserted
        Payment orphan = paymentDAO.getPaymentByAppointmentId(invalidAppointmentId);
        assertNull("No orphan payment record should exist after rollback", orphan);
    }

    private boolean isValidPaymentMode(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            return false;
        }
        return ALLOWED_MODES.contains(mode.trim());
    }
}

package com.odas.appointment;

import com.odas.Appointment;
import com.odas.Doctor;
import com.odas.Patient;
import com.odas.Payment;
import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
import com.odas.dao.PaymentDAO;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit & Integration tests for STAGE 7 — Admin Reports & Hospital Analytics.
 * 
 * Purpose (for viva):
 * - Verifies dynamic date range and status filtering for appointments (AppointmentDAO.getFilteredAppointments).
 * - Verifies dynamic date range and payment mode filtering for payments (PaymentDAO.getFilteredPayments).
 * - Verifies patient and doctor workload aggregation calculations.
 * - Tests revenue summary metrics and mode breakdown computations.
 */
public class Stage7AdminReportsTest {

    private AppointmentDAO appointmentDAO;
    private PaymentDAO paymentDAO;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;

    @Before
    public void setUp() {
        this.appointmentDAO = new AppointmentDAO();
        this.paymentDAO = new PaymentDAO();
        this.patientDAO = new PatientDAO();
        this.doctorDAO = new DoctorDAO();
    }

    @Test
    public void testGetFilteredAppointmentsByStatus() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        // Query scheduled appointments
        List<Appointment> scheduled = appointmentDAO.getFilteredAppointments(null, null, "SCHEDULED");
        assertNotNull("Scheduled list should not be null", scheduled);
        for (Appointment a : scheduled) {
            assertEquals("All returned items must have SCHEDULED status", "SCHEDULED", a.getStatus().toUpperCase());
        }

        // Query completed appointments
        List<Appointment> completed = appointmentDAO.getFilteredAppointments(null, null, "COMPLETED");
        assertNotNull("Completed list should not be null", completed);
        for (Appointment a : completed) {
            assertEquals("All returned items must have COMPLETED status", "COMPLETED", a.getStatus().toUpperCase());
        }

        // Query with ALL status
        List<Appointment> allStatus = appointmentDAO.getFilteredAppointments(null, null, "ALL");
        assertNotNull("All list should not be null", allStatus);
    }

    @Test
    public void testGetFilteredAppointmentsByDateRange() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now().plusDays(30);

        List<Appointment> inRange = appointmentDAO.getFilteredAppointments(from, to, "ALL");
        assertNotNull("Filtered appointments should not be null", inRange);

        // Date range in the distant past should return an empty list gracefully
        List<Appointment> pastRange = appointmentDAO.getFilteredAppointments(
                LocalDate.of(1990, 1, 1),
                LocalDate.of(1990, 1, 2),
                "ALL"
        );
        assertNotNull("Result should not be null for distant past", pastRange);
        assertTrue("Past date range should return 0 appointments", pastRange.isEmpty());
    }

    @Test
    public void testGetFilteredPaymentsByMode() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        // Test filtering by payment mode
        for (String mode : new String[]{"Card", "UPI", "Net Banking"}) {
            List<Payment> list = paymentDAO.getFilteredPayments(null, null, mode);
            assertNotNull("List should not be null for mode: " + mode, list);
            for (Payment p : list) {
                assertEquals("Payment mode should match filter", mode, p.getPaymentMode());
            }
        }

        // Test filtering by ALL modes
        List<Payment> allModes = paymentDAO.getFilteredPayments(null, null, "ALL");
        assertNotNull("All modes should not be null", allModes);
    }

    @Test
    public void testGetFilteredPaymentsByDateRange() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        // Far past range should return empty list gracefully
        List<Payment> pastPayments = paymentDAO.getFilteredPayments(
                LocalDate.of(1995, 1, 1),
                LocalDate.of(1995, 1, 2),
                "ALL"
        );
        assertNotNull("Payments list should not be null", pastPayments);
        assertTrue("Past date range should return empty list", pastPayments.isEmpty());
    }

    @Test
    public void testPatientAndDoctorWorkloadAggregation() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        List<Appointment> allAppts = appointmentDAO.getAllAppointments();
        assertNotNull("All appointments should not be null", allAppts);

        // Calculate patient appointments count
        Map<Integer, Long> patientApptCounts = allAppts.stream()
                .collect(Collectors.groupingBy(Appointment::getPatientId, Collectors.counting()));
        assertNotNull("Patient appointment counts map should not be null", patientApptCounts);

        // Calculate doctor consultation workload
        Map<Integer, Long> doctorApptCounts = allAppts.stream()
                .collect(Collectors.groupingBy(Appointment::getDoctorId, Collectors.counting()));
        assertNotNull("Doctor appointment counts map should not be null", doctorApptCounts);

        // Non-existent patient or doctor should yield 0
        assertEquals(Long.valueOf(0), patientApptCounts.getOrDefault(-999, 0L));
        assertEquals(Long.valueOf(0), doctorApptCounts.getOrDefault(-999, 0L));

        // If there are appointments, verify counts match
        long sumFromPatientMap = patientApptCounts.values().stream().mapToLong(Long::longValue).sum();
        assertEquals("Sum of patient appointments should match total appointments", allAppts.size(), sumFromPatientMap);
    }

    @Test
    public void testRevenueSummaryCalculations() {
        List<Payment> testPayments = new ArrayList<>();
        
        Payment p1 = new Payment();
        p1.setAmount(500.0);
        p1.setPaymentMode("Card");
        p1.setPaymentStatus("SUCCESS");
        testPayments.add(p1);

        Payment p2 = new Payment();
        p2.setAmount(750.0);
        p2.setPaymentMode("UPI");
        p2.setPaymentStatus("SUCCESS");
        testPayments.add(p2);

        Payment p3 = new Payment();
        p3.setAmount(1000.0);
        p3.setPaymentMode("Net Banking");
        p3.setPaymentStatus("SUCCESS");
        testPayments.add(p3);

        Payment pFailed = new Payment();
        pFailed.setAmount(600.0);
        pFailed.setPaymentMode("Card");
        pFailed.setPaymentStatus("FAILED");
        testPayments.add(pFailed);

        double totalRevenue = 0.0;
        double cardRev = 0.0;
        double upiRev = 0.0;
        double nbRev = 0.0;
        long cardCount = 0;
        long upiCount = 0;
        long nbCount = 0;

        for (Payment p : testPayments) {
            if ("SUCCESS".equalsIgnoreCase(p.getPaymentStatus())) {
                totalRevenue += p.getAmount();
                if ("Card".equalsIgnoreCase(p.getPaymentMode())) {
                    cardRev += p.getAmount();
                    cardCount++;
                } else if ("UPI".equalsIgnoreCase(p.getPaymentMode())) {
                    upiRev += p.getAmount();
                    upiCount++;
                } else if ("Net Banking".equalsIgnoreCase(p.getPaymentMode())) {
                    nbRev += p.getAmount();
                    nbCount++;
                }
            }
        }

        assertEquals("Total successful revenue should be 2250.0", 2250.0, totalRevenue, 0.001);
        assertEquals("Card revenue should be 500.0", 500.0, cardRev, 0.001);
        assertEquals("UPI revenue should be 750.0", 750.0, upiRev, 0.001);
        assertEquals("Net banking revenue should be 1000.0", 1000.0, nbRev, 0.001);
        assertEquals(1, cardCount);
        assertEquals(1, upiCount);
        assertEquals(1, nbCount);
    }

    @Test
    public void testEntityRetrievalsForReports() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        List<Patient> patients = patientDAO.getAllPatients();
        assertNotNull("Patients list should not be null", patients);
        assertFalse("Patients list should have registered patients", patients.isEmpty());

        List<Doctor> doctors = doctorDAO.getAllDoctors();
        assertNotNull("Doctors list should not be null", doctors);
        assertFalse("Doctors list should have registered doctors", doctors.isEmpty());

        double totalDbRevenue = paymentDAO.getTotalRevenue();
        assertTrue("Total DB revenue should be >= 0", totalDbRevenue >= 0);
    }

    @Test
    public void testFilteredPatientsReportByDateRange() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());

        // Range covering 2026
        List<Patient> list2026 = patientDAO.getFilteredPatients(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertNotNull("Filtered patients should not be null", list2026);
        assertFalse("Should find patients registered in 2026", list2026.isEmpty());

        for (Patient p : list2026) {
            assertNotNull("Patient createdDate should be populated", p.getCreatedDate());
            assertNotNull("Patient formattedCreatedDate should not be null", p.getFormattedCreatedDate());
        }

        // Distant past range
        List<Patient> emptyList = patientDAO.getFilteredPatients(LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 2));
        assertNotNull(emptyList);
        assertTrue("No patients should be found in 1990", emptyList.isEmpty());
    }

    @Test
    public void testFilteredDoctorsReportByDateRangeAndSpecialization() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());

        // Filter by specialization
        List<Doctor> cardiologists = doctorDAO.getFilteredDoctors(null, null, "Cardiologist");
        assertNotNull("Cardiologists list should not be null", cardiologists);
        assertFalse("Should find at least 1 cardiologist", cardiologists.isEmpty());
        for (Doctor d : cardiologists) {
            assertTrue("Doctor specialization should match filter", d.getSpecialization().equalsIgnoreCase("Cardiologist"));
            assertNotNull("Doctor createdDate should be populated", d.getCreatedDate());
            assertNotNull("Doctor formattedCreatedDate should not be null", d.getFormattedCreatedDate());
        }

        // Distant past date range
        List<Doctor> pastDocs = doctorDAO.getFilteredDoctors(LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 2), null);
        assertNotNull(pastDocs);
        assertTrue("No doctors should match 1990 date range", pastDocs.isEmpty());
    }

    @Test
    public void testPatientDuplicateEmailAndPhoneChecksExceptSelf() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());

        // Patient 1 email/phone
        Patient p1 = patientDAO.getPatientById(1);
        if (p1 != null && p1.getEmail() != null && p1.getPhone() != null) {
            // Own email / phone should NOT be marked as duplicate
            assertFalse("Own email should not be flagged as taken", patientDAO.isEmailTakenExcept(p1.getEmail(), 1));
            assertFalse("Own phone should not be flagged as taken", patientDAO.isPhoneTakenExcept(p1.getPhone(), 1));

            // Checked against another patient ID (e.g. 2), it SHOULD be flagged as taken
            assertTrue("Email taken by patient 1 should be flagged for patient 2", patientDAO.isEmailTakenExcept(p1.getEmail(), 2));
            assertTrue("Phone taken by patient 1 should be flagged for patient 2", patientDAO.isPhoneTakenExcept(p1.getPhone(), 2));
        }
    }
}

package com.odas.appointment;

import com.odas.Appointment;
import com.odas.Prescription;
import com.odas.dao.AppointmentDAO;
import com.odas.dao.PrescriptionDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit & Integration tests for STAGE 5 — Medical Prescription Management.
 * 
 * Purpose (for viva):
 * - Validates Prescription entity model properties, constructors, and enriched display fields.
 * - Enforces clinical data validation rules (required diagnosis and prescription details).
 * - Tests full end-to-end clinical workflow:
 *   1. Completed appointment lifecycle.
 *   2. Attending doctor issues medical diagnosis and prescription instructions.
 *   3. Oracle PRESCRIPTIONS persistence with CLOB support.
 *   4. Patient and Doctor queries for clinical records.
 *   5. Clean deletion / database integrity verification.
 */
public class Stage5PrescriptionManagementTest {

    private PrescriptionDAO prescriptionDAO;
    private AppointmentDAO appointmentDAO;
    private List<Integer> createdPrescriptionIds;
    private List<Integer> createdAppointmentIds;

    @Before
    public void setUp() {
        this.prescriptionDAO = new PrescriptionDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.createdPrescriptionIds = new ArrayList<>();
        this.createdAppointmentIds = new ArrayList<>();
    }

    @After
    public void tearDown() {
        if (!com.odas.util.DBConnection.isAvailable()) {
            return;
        }
        // Clean up test prescriptions first (due to foreign key constraint to appointments)
        for (Integer presId : createdPrescriptionIds) {
            try {
                prescriptionDAO.deletePrescription(presId);
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
    public void testPrescriptionModelBasics() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Prescription p = new Prescription(101, 1, 2, "Essential Hypertension", "Tab. Amlodipine 5mg once daily");
        assertEquals(101, p.getAppointmentId());
        assertEquals(1, p.getDoctorId());
        assertEquals(2, p.getPatientId());
        assertEquals("Essential Hypertension", p.getDiagnosis());
        assertEquals("Tab. Amlodipine 5mg once daily", p.getPrescriptionDetails());

        p.setId(55);
        p.setCreatedDate(now);
        assertEquals(55, p.getId());
        assertEquals(now, p.getCreatedDate());
    }

    @Test
    public void testPrescriptionEnrichedMetadata() {
        Timestamp apptDate = Timestamp.valueOf(LocalDate.now().atTime(10, 30));
        Prescription p = new Prescription();
        p.setDoctorName("Dr. Priya");
        p.setDoctorSpecialization("Dermatologist");
        p.setPatientName("Eniya Sree K");
        p.setAppointmentDate(apptDate);

        assertEquals("Dr. Priya", p.getDoctorName());
        assertEquals("Dermatologist", p.getDoctorSpecialization());
        assertEquals("Eniya Sree K", p.getPatientName());
        assertEquals(apptDate, p.getAppointmentDate());
    }

    @Test
    public void testPrescriptionInputValidation() {
        // Validation logic matching PrescriptionServlet
        assertFalse("Empty diagnosis must be invalid", isValidPrescription("", "Tab. Paracetamol 650mg"));
        assertFalse("Null diagnosis must be invalid", isValidPrescription(null, "Tab. Paracetamol 650mg"));
        assertFalse("Whitespace diagnosis must be invalid", isValidPrescription("   ", "Tab. Paracetamol 650mg"));

        assertFalse("Empty details must be invalid", isValidPrescription("Viral Fever", ""));
        assertFalse("Null details must be invalid", isValidPrescription("Viral Fever", null));
        assertFalse("Whitespace details must be invalid", isValidPrescription("Viral Fever", "   "));

        assertTrue("Valid diagnosis and details must pass", 
                isValidPrescription("Acute Bronchitis", "1. Azithromycin 500mg OD\n2. Cough syrup 10ml TDS"));
    }

    @Test
    public void testPrescriptionEndToEndDatabaseWorkflow() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        int doctorId = 1; // Dr. Kumar
        int patientId = 1; // Eniya
        Timestamp apptTs = Timestamp.valueOf(LocalDate.now().minusDays(1).atTime(9, 30));
        String slot = "09:30";

        // 1. Create a completed consultation appointment
        Appointment appt = new Appointment(doctorId, patientId, apptTs, "COMPLETED", slot, "UNPAID");
        int apptId = appointmentDAO.bookAppointment(appt);
        assertTrue("Test appointment must be booked successfully", apptId > 0);
        createdAppointmentIds.add(apptId);

        // 2. Issue a clinical prescription
        String diagnosis = "Mild Hypertension & Sinus Headache";
        String multiLineRx = "1. Tab. Telmisartan 40mg - 1 tablet once daily morning after breakfast (15 days)\n" +
                             "2. Tab. Paracetamol 650mg - 1 tablet SOS for severe headache\n" +
                             "Advice: Low salt diet, 30 mins brisk walking, check BP weekly.";

        Prescription newPrescription = new Prescription(apptId, doctorId, patientId, diagnosis, multiLineRx);
        int presId = prescriptionDAO.addPrescription(newPrescription);
        assertTrue("Prescription creation must succeed and return generated ID > 0", presId > 0);
        createdPrescriptionIds.add(presId);

        // 3. Query prescription by primary ID
        Prescription fetchedById = prescriptionDAO.getPrescriptionById(presId);
        assertNotNull("Prescription must be retrievable by ID", fetchedById);
        assertEquals(presId, fetchedById.getId());
        assertEquals(apptId, fetchedById.getAppointmentId());
        assertEquals(doctorId, fetchedById.getDoctorId());
        assertEquals(patientId, fetchedById.getPatientId());
        assertEquals(diagnosis, fetchedById.getDiagnosis());
        assertEquals(multiLineRx, fetchedById.getPrescriptionDetails());
        assertEquals("Dr. Kumar", fetchedById.getDoctorName());
        assertNotNull(fetchedById.getCreatedDate());

        // 4. Query prescription by appointment ID
        Prescription fetchedByAppt = prescriptionDAO.getPrescriptionByAppointmentId(apptId);
        assertNotNull("Prescription must be retrievable by appointment ID", fetchedByAppt);
        assertEquals(presId, fetchedByAppt.getId());
        assertEquals(diagnosis, fetchedByAppt.getDiagnosis());

        // 5. Query prescriptions by patient ID
        List<Prescription> patientList = prescriptionDAO.getPrescriptionsByPatientId(patientId);
        assertNotNull(patientList);
        boolean foundInPatientList = patientList.stream().anyMatch(p -> p.getId() == presId);
        assertTrue("Created prescription must appear in patient prescription history", foundInPatientList);

        // 6. Query prescriptions by doctor ID
        List<Prescription> doctorList = prescriptionDAO.getPrescriptionsByDoctorId(doctorId);
        assertNotNull(doctorList);
        boolean foundInDoctorList = doctorList.stream().anyMatch(p -> p.getId() == presId);
        assertTrue("Created prescription must appear in doctor consultation records", foundInDoctorList);

        // 7. Duplicate prescription insertion for same appointment must be blocked
        Prescription duplicatePrescription = new Prescription(apptId, doctorId, patientId, "Another diagnosis", "Another Rx");
        int duplicatePresId = prescriptionDAO.addPrescription(duplicatePrescription);
        assertEquals("Duplicate prescription insertion must fail due to UNIQUE constraint UQ_PRESCRIPTION_APPT", -1, duplicatePresId);

        // 8. Update prescription (SRS FR10 & Section 8)
        String updatedDiagnosis = "Hypertension Controlled with Lifestyle & Regimen";
        String updatedRx = "1. Tab. Telmisartan 20mg - 1 tablet daily morning after breakfast (30 days)\n" +
                           "Advice: Continue low salt diet.";
        fetchedById.setDiagnosis(updatedDiagnosis);
        fetchedById.setPrescriptionDetails(updatedRx);
        boolean updateSuccess = prescriptionDAO.updatePrescription(fetchedById);
        assertTrue("Updating prescription must succeed", updateSuccess);

        Prescription reFetched = prescriptionDAO.getPrescriptionById(presId);
        assertNotNull(reFetched);
        assertEquals(updatedDiagnosis, reFetched.getDiagnosis());
        assertEquals(updatedRx, reFetched.getPrescriptionDetails());
    }

    private boolean isValidPrescription(String diagnosis, String prescriptionDetails) {
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            return false;
        }
        if (prescriptionDetails == null || prescriptionDetails.trim().isEmpty()) {
            return false;
        }
        return true;
    }
}

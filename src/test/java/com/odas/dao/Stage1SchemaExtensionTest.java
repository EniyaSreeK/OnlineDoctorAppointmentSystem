package com.odas.dao;

import com.odas.Appointment;
import com.odas.Doctor;
import com.odas.Patient;
import com.odas.Payment;
import com.odas.Prescription;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit and integration tests for STAGE 1 — Database Schema Extension.
 * Verifies models, getters/setters, and DAO queries with extended SRS data fields.
 */
public class Stage1SchemaExtensionTest {

    @Test
    public void testPatientExtendedModel() {
        Patient p = new Patient(10, "Aarav Sharma", 28, 5, "Male", "9876543210", "aarav@example.com", "123 Anna Nagar, Chennai");
        assertEquals(10, p.getId());
        assertEquals("Aarav Sharma", p.getName());
        assertEquals(28, p.getAge());
        assertEquals(Integer.valueOf(5), p.getUserId());
        assertEquals("Male", p.getGender());
        assertEquals("9876543210", p.getPhone());
        assertEquals("aarav@example.com", p.getEmail());
        assertEquals("123 Anna Nagar, Chennai", p.getAddress());

        p.setGender("Other");
        p.setPhone("9988776655");
        assertEquals("Other", p.getGender());
        assertEquals("9988776655", p.getPhone());
    }

    @Test
    public void testDoctorExtendedModel() {
        Doctor d = new Doctor(1, "Dr. Kumar", "Cardiologist", 2,
                "MBBS, MD (Cardiology)", 12, "9876543210", "dr.kumar@medicare.com",
                "Apollo Block A, Chennai", "Mon,Wed,Fri", "09:00-13:00");

        assertEquals(1, d.getId());
        assertEquals("Dr. Kumar", d.getName());
        assertEquals("Cardiologist", d.getSpecialization());
        assertEquals("MBBS, MD (Cardiology)", d.getQualification());
        assertEquals(Integer.valueOf(12), d.getExperience());
        assertEquals("9876543210", d.getPhone());
        assertEquals("dr.kumar@medicare.com", d.getEmail());
        assertEquals("Apollo Block A, Chennai", d.getLocation());
        assertEquals("Mon,Wed,Fri", d.getAvailableDays());
        assertEquals("09:00-13:00", d.getAvailableTime());
        assertEquals(500.0, d.getConsultationFee(), 0.001);

        d.setConsultationFee(750.0);
        assertEquals(750.0, d.getConsultationFee(), 0.001);
    }

    @Test
    public void testAppointmentExtendedModel() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Appointment a = new Appointment(100, 1, 5, now, "SCHEDULED", "14:30", "UNPAID");

        assertEquals(100, a.getId());
        assertEquals(1, a.getDoctorId());
        assertEquals(5, a.getPatientId());
        assertEquals("SCHEDULED", a.getStatus());
        assertEquals("14:30", a.getAppointmentTime());
        assertEquals("02:30 PM", a.getFormattedTime());
        assertEquals("UNPAID", a.getPaymentStatus());

        a.setPaymentStatus("PAID");
        assertEquals("PAID", a.getPaymentStatus());

        Appointment defaultAppt = new Appointment();
        assertEquals("10:00", defaultAppt.getAppointmentTime());
        assertEquals("10:00 AM", defaultAppt.getFormattedTime());
    }

    @Test
    public void testPrescriptionModel() {
        Prescription pr = new Prescription(1, 100, 2, 5, "Acute Dermatitis", "Apply Hydrocortisone cream twice daily.", new Timestamp(System.currentTimeMillis()));
        assertEquals(1, pr.getId());
        assertEquals(100, pr.getAppointmentId());
        assertEquals(2, pr.getDoctorId());
        assertEquals(5, pr.getPatientId());
        assertEquals("Acute Dermatitis", pr.getDiagnosis());
        assertTrue(pr.getPrescriptionDetails().contains("Hydrocortisone"));
    }

    @Test
    public void testPaymentModel() {
        Payment pay = new Payment(1, 100, 5, 500.0, "UPI", "SUCCESS", new Timestamp(System.currentTimeMillis()));
        assertEquals(1, pay.getId());
        assertEquals(100, pay.getAppointmentId());
        assertEquals(5, pay.getPatientId());
        assertEquals(500.0, pay.getAmount(), 0.001);
        assertEquals("UPI", pay.getPaymentMode());
        assertEquals("SUCCESS", pay.getPaymentStatus());
    }

    @Test
    public void testDoctorDAOExtendedFieldsFromDatabase() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        DoctorDAO dao = new DoctorDAO();
        List<Doctor> list = dao.getAllDoctors();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        Doctor d = list.get(0);
        assertNotNull(d.getName());
        assertNotNull(d.getSpecialization());
        assertTrue("Doctor consultation fee must be positive", d.getConsultationFee() > 0);
        // Verify extended fields loaded from DB
        System.out.println("Loaded doctor from DB: " + d.getName() + ", Qual=" + d.getQualification() + 
                           ", Exp=" + d.getExperience() + ", Days=" + d.getAvailableDays() + 
                           ", Time=" + d.getAvailableTime() + ", Fee=" + d.getConsultationFee());
    }
}

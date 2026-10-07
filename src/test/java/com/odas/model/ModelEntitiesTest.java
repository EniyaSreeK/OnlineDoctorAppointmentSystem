package com.odas.model;

import com.odas.Appointment;
import com.odas.Doctor;
import com.odas.Patient;
import com.odas.Payment;
import com.odas.Prescription;
import com.odas.User;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Pure Unit Tests verifying domain models and business entity behaviors:
 * Doctor, Patient, Appointment, Payment, Prescription, and User.
 */
public class ModelEntitiesTest {

    @Test
    public void testDoctorEntity() {
        Doctor doc = new Doctor();
        doc.setId(10);
        doc.setName("Dr. Rajesh Sharma");
        doc.setSpecialization("Cardiology");
        doc.setUserId(25);
        doc.setQualification("MBBS, MD (Cardio)");
        doc.setExperience(14);
        doc.setPhone("9876543210");
        doc.setEmail("rajesh.sharma@hospital.org");
        doc.setLocation("Apollo Hospital, Chennai");
        doc.setAvailableDays("Mon,Wed,Fri");
        doc.setAvailableTime("09:00-13:00");
        doc.setConsultationFee(750.0);

        assertEquals(10, doc.getId());
        assertEquals("Dr. Rajesh Sharma", doc.getName());
        assertEquals("Cardiology", doc.getSpecialization());
        assertEquals(Integer.valueOf(25), doc.getUserId());
        assertEquals("MBBS, MD (Cardio)", doc.getQualification());
        assertEquals(Integer.valueOf(14), doc.getExperience());
        assertEquals("9876543210", doc.getPhone());
        assertEquals("rajesh.sharma@hospital.org", doc.getEmail());
        assertEquals("Apollo Hospital, Chennai", doc.getLocation());
        assertEquals("Mon,Wed,Fri", doc.getAvailableDays());
        assertEquals("09:00-13:00", doc.getAvailableTime());
        assertEquals(750.0, doc.getConsultationFee(), 0.001);

        // Parameterized constructor
        Doctor docParam = new Doctor("Dr. Kumar", "Pediatrics", 30);
        assertEquals("Dr. Kumar", docParam.getName());
        assertEquals("Pediatrics", docParam.getSpecialization());
        assertEquals(Integer.valueOf(30), docParam.getUserId());
    }

    @Test
    public void testPatientEntity() {
        Patient p = new Patient();
        p.setId(42);
        p.setName("Ananya Iyer");
        p.setAge(28);
        p.setUserId(15);
        p.setGender("FEMALE");
        p.setPhone("9123456789");
        p.setEmail("ananya@example.com");
        p.setAddress("45 North Usman Road, Chennai");

        assertEquals(42, p.getId());
        assertEquals("Ananya Iyer", p.getName());
        assertEquals(28, p.getAge());
        assertEquals(Integer.valueOf(15), p.getUserId());
        assertEquals("FEMALE", p.getGender());
        assertEquals("9123456789", p.getPhone());
        assertEquals("ananya@example.com", p.getEmail());
        assertEquals("45 North Usman Road, Chennai", p.getAddress());

        // Parameterized constructor
        Patient pParam = new Patient("Karthik", 35, 16);
        assertEquals("Karthik", pParam.getName());
        assertEquals(35, pParam.getAge());
        assertEquals(Integer.valueOf(16), pParam.getUserId());
    }

    @Test
    public void testAppointmentEntityAndFormatting() {
        Timestamp apptTs = Timestamp.valueOf(LocalDateTime.of(2026, 11, 20, 10, 30));
        Appointment appt = new Appointment(101, 1, 2, apptTs, "SCHEDULED");
        appt.setAppointmentTime("10:30");
        appt.setPaymentStatus("UNPAID");
        appt.setReminderSent(0);
        appt.setDoctorName("Dr. Kumar");
        appt.setDoctorSpecialization("Cardiologist");
        appt.setPatientName("Eniya Sree");

        assertEquals(101, appt.getId());
        assertEquals(1, appt.getDoctorId());
        assertEquals(2, appt.getPatientId());
        assertEquals(apptTs, appt.getAppointmentDate());
        assertEquals("SCHEDULED", appt.getStatus());
        assertEquals("10:30", appt.getAppointmentTime());
        assertEquals("UNPAID", appt.getPaymentStatus());
        assertEquals(0, appt.getReminderSent());
        assertEquals("Dr. Kumar", appt.getDoctorName());
        assertEquals("Cardiologist", appt.getDoctorSpecialization());
        assertEquals("Eniya Sree", appt.getPatientName());
        assertEquals("10:30 AM", appt.getFormattedTime());

        // Afternoon slot formatting
        appt.setAppointmentTime("15:00");
        assertEquals("03:00 PM", appt.getFormattedTime());

        // Full parameterized constructor
        Appointment fullAppt = new Appointment(1, 2, apptTs, "SCHEDULED", "11:00", "PAID");
        assertEquals("11:00", fullAppt.getAppointmentTime());
        assertEquals("PAID", fullAppt.getPaymentStatus());
    }

    @Test
    public void testPaymentEntity() {
        Timestamp paymentDate = Timestamp.valueOf(LocalDateTime.of(2026, 10, 6, 14, 15));
        Payment payment = new Payment(1, 101, 2, 650.0, "UPI", "SUCCESS", paymentDate);
        payment.setDoctorName("Dr. Priya");
        payment.setDoctorSpecialization("Dermatology");
        payment.setPatientName("Aarav");
        payment.setAppointmentDate(Timestamp.valueOf(LocalDate.of(2026, 10, 10).atStartOfDay()));
        payment.setAppointmentTime("11:30");

        assertEquals(1, payment.getId());
        assertEquals(101, payment.getAppointmentId());
        assertEquals(2, payment.getPatientId());
        assertEquals(650.0, payment.getAmount(), 0.001);
        assertEquals("UPI", payment.getPaymentMode());
        assertEquals("SUCCESS", payment.getPaymentStatus());
        assertEquals(paymentDate, payment.getPaymentDate());
        assertEquals("Dr. Priya", payment.getDoctorName());
        assertEquals("Dermatology", payment.getDoctorSpecialization());
        assertEquals("Aarav", payment.getPatientName());
        assertEquals("11:30 AM", payment.getFormattedTime());
    }

    @Test
    public void testPrescriptionEntity() {
        Prescription rx = new Prescription(101, 1, 2, "Acute Pharyngitis", "Tab. Amoxicillin 500mg TDS x 5 days");
        rx.setId(88);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        rx.setCreatedDate(now);
        rx.setDoctorName("Dr. Anita");
        rx.setDoctorSpecialization("ENT");
        rx.setPatientName("Meera");
        rx.setAppointmentDate(now);

        assertEquals(88, rx.getId());
        assertEquals(101, rx.getAppointmentId());
        assertEquals(1, rx.getDoctorId());
        assertEquals(2, rx.getPatientId());
        assertEquals("Acute Pharyngitis", rx.getDiagnosis());
        assertEquals("Tab. Amoxicillin 500mg TDS x 5 days", rx.getPrescriptionDetails());
        assertEquals(now, rx.getCreatedDate());
        assertEquals("Dr. Anita", rx.getDoctorName());
        assertEquals("ENT", rx.getDoctorSpecialization());
        assertEquals("Meera", rx.getPatientName());
        assertEquals(now, rx.getAppointmentDate());
    }

    @Test
    public void testUserEntity() {
        User user = new User(5, "doctor_kumar", "hashed_secret", "DOCTOR");
        assertEquals(5, user.getId());
        assertEquals("doctor_kumar", user.getUsername());
        assertEquals("hashed_secret", user.getPassword());
        assertEquals("DOCTOR", user.getRole());

        user.setId(10);
        user.setUsername("admin_lead");
        user.setPassword("new_hash");
        user.setRole("ADMIN");

        assertEquals(10, user.getId());
        assertEquals("admin_lead", user.getUsername());
        assertEquals("new_hash", user.getPassword());
        assertEquals("ADMIN", user.getRole());

        User newUser = new User("patient_xyz", "hashed_pwd", "PATIENT");
        assertEquals("patient_xyz", newUser.getUsername());
        assertEquals("PATIENT", newUser.getRole());
    }
}

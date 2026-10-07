package com.odas.dao;

import com.odas.Appointment;
import com.odas.Doctor;
import com.odas.Patient;
import com.odas.User;
import com.odas.util.DBConnection;
import com.odas.util.PasswordUtil;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.Assert.*;

/**
 * Verification tests for Stage 1.
 * Tests configuration loading, models, password hashing, and DB connectivity test.
 */
public class DatabaseVerificationTest {

    @Test
    public void testPasswordHashing() {
        String raw = "admin123";
        String hashed = PasswordUtil.hashPassword(raw);
        assertNotNull(hashed);
        assertTrue("PBKDF2 hash should start with prefix", hashed.startsWith(PasswordUtil.PREFIX));
        assertTrue(PasswordUtil.checkPassword("admin123", hashed));
        assertFalse(PasswordUtil.checkPassword("wrongpass", hashed));
    }

    @Test
    public void testPropertiesInClasspath() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("db.properties");
        if (stream == null) {
            stream = getClass().getClassLoader().getResourceAsStream("db.properties.example");
        }
        assertNotNull("db.properties or db.properties.example must be available in classpath", stream);

        InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("schema.sql");
        assertNotNull("schema.sql must be available in classpath", schemaStream);
    }

    @Test
    public void testModels() {
        User user = new User(1, "patient1", "pass123", "PATIENT");
        assertEquals(1, user.getId());
        assertEquals("patient1", user.getUsername());
        assertEquals("PATIENT", user.getRole());

        Doctor doctor = new Doctor(101, "Dr. Kumar", "Cardiologist", 2);
        assertEquals(101, doctor.getId());
        assertEquals("Dr. Kumar", doctor.getName());
        assertEquals("Cardiologist", doctor.getSpecialization());

        Patient patient = new Patient(201, "John Doe", 35, 1);
        assertEquals(201, patient.getId());
        assertEquals("John Doe", patient.getName());
        assertEquals(35, patient.getAge());

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Appointment appt = new Appointment(1, doctor.getId(), patient.getId(), now, "SCHEDULED");
        appt.setDoctor(doctor);
        appt.setPatient(patient);
        assertEquals(1, appt.getId());
        assertEquals("Dr. Kumar", appt.getDoctorName());
        assertEquals("John Doe", appt.getPatientName());
        assertEquals("SCHEDULED", appt.getStatus());
    }

    @Test
    public void testOracleDriverLoaded() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            assertTrue("Oracle JDBC Driver loaded successfully", true);
        } catch (ClassNotFoundException e) {
            fail("Oracle JDBC Driver (ojdbc11) not found in classpath: " + e.getMessage());
        }
    }
}

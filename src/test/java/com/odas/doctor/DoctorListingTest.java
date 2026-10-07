package com.odas.doctor;

import com.odas.Doctor;
import com.odas.dao.DoctorDAO;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for Stage 3 Doctor listing models and dynamic attributes.
 */
public class DoctorListingTest {

    @Test
    public void testDoctorAttributes() {
        Doctor doctor = new Doctor(1, "Dr. Kumar", "Cardiologist", 10);
        assertEquals(1, doctor.getId());
        assertEquals("Dr. Kumar", doctor.getName());
        assertEquals("Cardiologist", doctor.getSpecialization());
        assertEquals(Integer.valueOf(10), doctor.getUserId());

        // Setter checks
        doctor.setName("Dr. Kumar Updated");
        doctor.setSpecialization("Interventional Cardiology");
        assertEquals("Dr. Kumar Updated", doctor.getName());
        assertEquals("Interventional Cardiology", doctor.getSpecialization());
    }

    @Test
    public void testDoctorCollection() {
        List<Doctor> doctors = new ArrayList<>();
        doctors.add(new Doctor(1, "Dr. Kumar", "Cardiologist", 10));
        doctors.add(new Doctor(2, "Dr. Priya", "Dermatologist", 11));
        doctors.add(new Doctor(3, "Dr. Sharma", "Pediatrician", 12));

        assertEquals(3, doctors.size());
        assertEquals("Dr. Priya", doctors.get(1).getName());
        assertEquals("Dermatologist", doctors.get(1).getSpecialization());
    }

    @Test
    public void testDoctorDAODatabaseFetch() {
        org.junit.Assume.assumeTrue("Oracle DB is reachable", com.odas.util.DBConnection.isAvailable());
        DoctorDAO dao = new DoctorDAO();
        List<Doctor> list = dao.getAllDoctors();
        assertNotNull("Doctors list should not be null", list);
        System.out.println("Loaded " + list.size() + " doctors from database:");
        for (Doctor d : list) {
            System.out.println(" - ID=" + d.getId() + ", Name=" + d.getName() + ", Spec=" + d.getSpecialization());
        }
        assertTrue("Should have doctors in database", list.size() >= 3);
    }
}

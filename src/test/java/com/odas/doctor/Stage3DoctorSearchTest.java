package com.odas.doctor;

import com.odas.Doctor;
import com.odas.dao.DoctorDAO;
import com.odas.util.DBConnection;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit & Integration tests for STAGE 3 — Dynamic Doctor Search & Parameterized Querying.
 * 
 * Purpose (for viva):
 * - Verifies multi-attribute filtering (name, specialization, location, available day).
 * - Verifies case-insensitive matching across database entries.
 * - Confirms parameterized SQL execution prevents SQL injection vulnerabilities.
 * - Asserts zero regression when filters are omitted or combined.
 */
public class Stage3DoctorSearchTest {

    private DoctorDAO doctorDAO;

    @BeforeClass
    public static void setUpClass() {
        Assume.assumeTrue("Oracle DB is reachable", DBConnection.isAvailable());
    }

    @Before
    public void setUp() {
        this.doctorDAO = new DoctorDAO();
    }

    @Test
    public void testSearchAllWhenFiltersAreNullOrEmpty() {
        List<Doctor> allNull = doctorDAO.searchDoctors(null, null, null, null);
        assertNotNull("Result should not be null", allNull);
        assertTrue("Should return all seeded doctors (at least 3)", allNull.size() >= 3);

        List<Doctor> allEmpty = doctorDAO.searchDoctors("   ", "", "  ", "");
        assertNotNull("Result should not be null", allEmpty);
        assertEquals("Empty filters should return same count as null filters", allNull.size(), allEmpty.size());
    }

    @Test
    public void testSearchByName() {
        // Exact name
        List<Doctor> resultKumar = doctorDAO.searchDoctors("Kumar", null, null, null);
        assertFalse("Should find Dr. Kumar", resultKumar.isEmpty());
        assertTrue("Doctor name should contain Kumar", resultKumar.get(0).getName().contains("Kumar"));

        // Case-insensitive test
        List<Doctor> lowerKumar = doctorDAO.searchDoctors("kumar", null, null, null);
        assertFalse("Case-insensitive lowercase 'kumar' should find match", lowerKumar.isEmpty());
        assertEquals("Kumar", lowerKumar.get(0).getName().replace("Dr. ", "").trim());

        List<Doctor> upperKumar = doctorDAO.searchDoctors("KUMAR", null, null, null);
        assertFalse("Uppercase 'KUMAR' should find match", upperKumar.isEmpty());

        // Partial match
        List<Doctor> partialPriya = doctorDAO.searchDoctors("Pri", null, null, null);
        assertFalse("Partial name 'Pri' should find Dr. Priya", partialPriya.isEmpty());
        assertTrue(partialPriya.get(0).getName().contains("Priya"));
    }

    @Test
    public void testSearchBySpecialization() {
        // Search Cardiologist
        List<Doctor> cardio = doctorDAO.searchDoctors(null, "Cardio", null, null);
        assertFalse("Should find Cardiologist", cardio.isEmpty());
        assertTrue(cardio.get(0).getSpecialization().toLowerCase().contains("cardio"));

        // Search Dermatologist (case-insensitive)
        List<Doctor> derm = doctorDAO.searchDoctors(null, "dermatologist", null, null);
        assertFalse("Should find Dermatologist", derm.isEmpty());
        assertEquals("Dermatologist", derm.get(0).getSpecialization());

        // Search Pediatrician
        List<Doctor> peds = doctorDAO.searchDoctors(null, "Pediatrician", null, null);
        assertFalse("Should find Pediatrician", peds.isEmpty());
        assertEquals("Pediatrician", peds.get(0).getSpecialization());
    }

    @Test
    public void testSearchByLocation() {
        // Search 'Apollo'
        List<Doctor> apollo = doctorDAO.searchDoctors(null, null, "Apollo", null);
        assertFalse("Should find doctor at Apollo", apollo.isEmpty());
        assertTrue(apollo.get(0).getLocation().contains("Apollo"));

        // Search 'SkinCare'
        List<Doctor> skin = doctorDAO.searchDoctors(null, null, "SkinCare", null);
        assertFalse("Should find doctor at SkinCare", skin.isEmpty());
        assertTrue(skin.get(0).getLocation().contains("SkinCare"));

        // Search 'Chennai' (all default sample doctors are in Chennai)
        List<Doctor> chennai = doctorDAO.searchDoctors(null, null, "Chennai", null);
        assertTrue("Multiple doctors should match Chennai location", chennai.size() >= 3);
    }

    @Test
    public void testSearchByAvailableDay() {
        // 'Mon' matches Dr. Kumar ('Mon,Wed,Fri') and Dr. Sharma ('Mon,Tue,Thu,Fri')
        List<Doctor> mondayDocs = doctorDAO.searchDoctors(null, null, null, "Mon");
        assertTrue("At least 2 doctors available on Monday", mondayDocs.size() >= 2);
        for (Doctor d : mondayDocs) {
            assertTrue("Doctor must have 'Mon' in available days: " + d.getAvailableDays(),
                    d.getAvailableDays() != null && d.getAvailableDays().contains("Mon"));
        }

        // 'Sat' matches Dr. Priya ('Tue,Thu,Sat')
        List<Doctor> saturdayDocs = doctorDAO.searchDoctors(null, null, null, "Sat");
        assertFalse("Should find doctor available on Saturday", saturdayDocs.isEmpty());
        assertTrue(saturdayDocs.stream().anyMatch(d -> d.getName().contains("Priya")));
    }

    @Test
    public void testSearchMultipleFiltersCombined() {
        // Combined matching filters: Dr. Priya + Dermatologist + SkinCare + Tue
        List<Doctor> matching = doctorDAO.searchDoctors("Priya", "Dermatologist", "SkinCare", "Tue");
        assertEquals("Should match exactly 1 doctor", 1, matching.size());
        assertEquals("Dr. Priya", matching.get(0).getName());

        // Conflicting filters: name Dr. Priya but specialization Cardiologist
        List<Doctor> conflict = doctorDAO.searchDoctors("Priya", "Cardiologist", null, null);
        assertTrue("Contradictory filters should return 0 results", conflict.isEmpty());
    }

    @Test
    public void testSearchNoMatches() {
        List<Doctor> none = doctorDAO.searchDoctors("NonExistentDocXYZ999", null, null, null);
        assertNotNull("Should return empty list, not null", none);
        assertTrue("No results expected", none.isEmpty());
    }

    @Test
    public void testSearchSqlInjectionSafety() {
        // Classic SQL injection payloads must be safely treated as literal strings
        List<Doctor> payload1 = doctorDAO.searchDoctors("' OR '1'='1", null, null, null);
        assertNotNull(payload1);
        assertTrue("SQL injection payload should return 0 matches safely", payload1.isEmpty());

        List<Doctor> payload2 = doctorDAO.searchDoctors("'; DROP TABLE DOCTORS; --", null, null, null);
        assertNotNull(payload2);
        assertTrue("SQL injection payload should return 0 matches safely", payload2.isEmpty());

        // Verify database integrity: DOCTORS table still intact and accessible
        List<Doctor> all = doctorDAO.getAllDoctors();
        assertTrue("Doctors table must remain intact", all.size() >= 3);
    }
}

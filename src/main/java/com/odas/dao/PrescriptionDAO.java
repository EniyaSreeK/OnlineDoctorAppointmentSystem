package com.odas.dao;

import com.odas.Prescription;
import com.odas.util.DBConnection;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for PRESCRIPTIONS table in Oracle 21c XE.
 * 
 * Purpose (for viva):
 * - Manages persistence operations for medical prescriptions and clinical notes.
 * - Handles Oracle CLOB fields for large multi-line prescription and dosage details.
 * - Enforces parameter binding via PreparedStatement to avoid SQL injection vulnerabilities.
 */
public class PrescriptionDAO {

    private static final Logger LOGGER = Logger.getLogger(PrescriptionDAO.class.getName());

    private static final String SELECT_PRESCRIPTION_COLUMNS = 
            "p.ID, p.APPOINTMENT_ID, p.DOCTOR_ID, p.PATIENT_ID, p.DIAGNOSIS, p.PRESCRIPTION_DETAILS, p.CREATED_DATE, " +
            "d.NAME AS DOCTOR_NAME, d.SPECIALIZATION AS DOCTOR_SPEC, " +
            "pt.NAME AS PATIENT_NAME, a.APPOINTMENT_DATE ";

    /**
     * Adds a new prescription record and returns the generated primary key.
     * 
     * @param prescription Prescription entity
     * @return generated ID or -1 on failure
     */
    public int addPrescription(Prescription prescription) {
        String sql = "INSERT INTO PRESCRIPTIONS (APPOINTMENT_ID, DOCTOR_ID, PATIENT_ID, DIAGNOSIS, PRESCRIPTION_DETAILS) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            ps.setInt(1, prescription.getAppointmentId());
            ps.setInt(2, prescription.getDoctorId());
            ps.setInt(3, prescription.getPatientId());
            ps.setString(4, prescription.getDiagnosis());
            ps.setString(5, prescription.getPrescriptionDetails());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        prescription.setId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding prescription for appointment: " + prescription.getAppointmentId(), e);
        }
        return -1;
    }

    /**
     * Retrieves a prescription by its primary key.
     * 
     * @param id prescription ID
     * @return Prescription entity or null
     */
    public Prescription getPrescriptionById(int id) {
        String sql = "SELECT " + SELECT_PRESCRIPTION_COLUMNS +
                     "FROM PRESCRIPTIONS p " +
                     "JOIN DOCTORS d ON p.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS pt ON p.PATIENT_ID = pt.ID " +
                     "JOIN APPOINTMENTS a ON p.APPOINTMENT_ID = a.ID " +
                     "WHERE p.ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPrescription(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving prescription by id: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves a prescription for a specific appointment.
     * 
     * @param appointmentId appointment ID
     * @return Prescription entity or null if not yet prescribed
     */
    public Prescription getPrescriptionByAppointmentId(int appointmentId) {
        String sql = "SELECT " + SELECT_PRESCRIPTION_COLUMNS +
                     "FROM PRESCRIPTIONS p " +
                     "JOIN DOCTORS d ON p.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS pt ON p.PATIENT_ID = pt.ID " +
                     "JOIN APPOINTMENTS a ON p.APPOINTMENT_ID = a.ID " +
                     "WHERE p.APPOINTMENT_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPrescription(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving prescription by appointmentId: " + appointmentId, e);
        }
        return null;
    }

    /**
     * Retrieves all prescriptions for a specific patient.
     * 
     * @param patientId patient ID
     * @return List of Prescription entities
     */
    public List<Prescription> getPrescriptionsByPatientId(int patientId) {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_PRESCRIPTION_COLUMNS +
                     "FROM PRESCRIPTIONS p " +
                     "JOIN DOCTORS d ON p.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS pt ON p.PATIENT_ID = pt.ID " +
                     "JOIN APPOINTMENTS a ON p.APPOINTMENT_ID = a.ID " +
                     "WHERE p.PATIENT_ID = ? " +
                     "ORDER BY p.CREATED_DATE DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPrescription(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving prescriptions for patientId: " + patientId, e);
        }
        return list;
    }

    /**
     * Retrieves all prescriptions created by a specific doctor.
     * 
     * @param doctorId doctor ID
     * @return List of Prescription entities
     */
    public List<Prescription> getPrescriptionsByDoctorId(int doctorId) {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_PRESCRIPTION_COLUMNS +
                     "FROM PRESCRIPTIONS p " +
                     "JOIN DOCTORS d ON p.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS pt ON p.PATIENT_ID = pt.ID " +
                     "JOIN APPOINTMENTS a ON p.APPOINTMENT_ID = a.ID " +
                     "WHERE p.DOCTOR_ID = ? " +
                     "ORDER BY p.CREATED_DATE DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPrescription(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving prescriptions for doctorId: " + doctorId, e);
        }
        return list;
    }

    /**
     * Updates an existing prescription record.
     * Enforces SRS FR10 & Section 8: "Only doctors can create or update digital prescriptions."
     * 
     * @param prescription Prescription entity with updated values
     * @return true if updated successfully, false otherwise
     */
    public boolean updatePrescription(Prescription prescription) {
        String sql = "UPDATE PRESCRIPTIONS SET DIAGNOSIS = ?, PRESCRIPTION_DETAILS = ? WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, prescription.getDiagnosis());
            ps.setString(2, prescription.getPrescriptionDetails());
            ps.setInt(3, prescription.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating prescription id: " + prescription.getId(), e);
        }
        return false;
    }

    /**
     * Deletes a prescription record by ID.
     * 
     * @param id prescription ID
     * @return true if deleted, false otherwise
     */
    public boolean deletePrescription(int id) {
        String sql = "DELETE FROM PRESCRIPTIONS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting prescription id: " + id, e);
        }
        return false;
    }

    private Prescription mapRowToPrescription(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getInt("ID"));
        p.setAppointmentId(rs.getInt("APPOINTMENT_ID"));
        p.setDoctorId(rs.getInt("DOCTOR_ID"));
        p.setPatientId(rs.getInt("PATIENT_ID"));
        p.setDiagnosis(rs.getString("DIAGNOSIS"));
        
        // Safely extract CLOB content
        Clob clob = rs.getClob("PRESCRIPTION_DETAILS");
        if (clob != null) {
            StringBuilder sb = new StringBuilder();
            try (Reader reader = clob.getCharacterStream();
                 BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(line);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error reading prescription CLOB", ex);
            }
            p.setPrescriptionDetails(sb.toString());
        } else {
            p.setPrescriptionDetails(rs.getString("PRESCRIPTION_DETAILS"));
        }

        p.setCreatedDate(rs.getTimestamp("CREATED_DATE"));
        p.setDoctorName(rs.getString("DOCTOR_NAME"));
        p.setDoctorSpecialization(rs.getString("DOCTOR_SPEC"));
        p.setPatientName(rs.getString("PATIENT_NAME"));
        p.setAppointmentDate(rs.getTimestamp("APPOINTMENT_DATE"));
        return p;
    }
}

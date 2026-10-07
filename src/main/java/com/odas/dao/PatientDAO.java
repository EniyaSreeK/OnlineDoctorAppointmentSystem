package com.odas.dao;

import com.odas.Patient;
import com.odas.util.DBConnection;

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
 * Data Access Object (DAO) for PATIENTS table in Oracle 21c XE.
 * 
 * Purpose (for viva):
 * - Manages patient records, connecting demographic data (name, age, gender, contact) with authentication accounts (user_id).
 * - Enables retrieval of patient information during appointment booking, profile updates, and dashboard views.
 * - Enforces parameter binding via PreparedStatement to avoid SQL injection.
 */
public class PatientDAO {

    private static final Logger LOGGER = Logger.getLogger(PatientDAO.class.getName());

    private static final String SELECT_COLUMNS = "ID, NAME, AGE, USER_ID, GENDER, PHONE, EMAIL, ADDRESS, CREATED_DATE";

    /**
     * Retrieves a patient by primary key ID.
     * 
     * @param id patient ID
     * @return Patient entity or null if not found
     */
    public Patient getPatientById(int id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM PATIENTS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPatient(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving patient by id: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves a patient associated with a specific user account ID.
     * 
     * @param userId user account ID
     * @return Patient entity or null if not found
     */
    public Patient getPatientByUserId(int userId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM PATIENTS WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPatient(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving patient by userId: " + userId, e);
        }
        return null;
    }

    /**
     * Inserts a new patient record and returns generated ID.
     * 
     * @param patient Patient entity to insert
     * @return generated ID or -1 on failure
     */
    public int addPatient(Patient patient) {
        String sql = "INSERT INTO PATIENTS (NAME, AGE, USER_ID, GENDER, PHONE, EMAIL, ADDRESS) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            ps.setString(1, patient.getName());
            ps.setInt(2, patient.getAge());
            ps.setInt(3, patient.getUserId());
            ps.setString(4, patient.getGender());
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getEmail());
            ps.setString(7, patient.getAddress());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        patient.setId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding patient: " + patient.getName(), e);
        }
        return -1;
    }

    /**
     * Updates an existing patient's profile details.
     * 
     * @param patient Patient entity with updated details
     * @return true if updated, false otherwise
     */
    public boolean updatePatient(Patient patient) {
        String sql = "UPDATE PATIENTS SET NAME = ?, AGE = ?, GENDER = ?, PHONE = ?, EMAIL = ?, ADDRESS = ? " +
                     "WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, patient.getName());
            ps.setInt(2, patient.getAge());
            ps.setString(3, patient.getGender());
            ps.setString(4, patient.getPhone());
            ps.setString(5, patient.getEmail());
            ps.setString(6, patient.getAddress());
            ps.setInt(7, patient.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating patient id: " + patient.getId(), e);
        }
        return false;
    }

    /**
     * Retrieves all registered patients.
     * 
     * @return List of Patient entities
     */
    public List<Patient> getAllPatients() {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLUMNS + " FROM PATIENTS ORDER BY ID ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToPatient(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all patients", e);
        }
        return list;
    }

    /**
     * Deletes a patient by ID.
     * 
     * @param id patient ID
     * @return true if deleted, false otherwise
     */
    public boolean deletePatient(int id) {
        String sql = "DELETE FROM PATIENTS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) {
                LOGGER.log(Level.WARNING, "Cannot delete patient id " + id + " due to active foreign key references (appointments/prescriptions/payments).");
                return false;
            }
            LOGGER.log(Level.SEVERE, "Error deleting patient id: " + id, e);
        }
        return false;
    }

    public boolean isEmailTakenExcept(String email, int excludePatientId) {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM PATIENTS WHERE LOWER(EMAIL) = ? AND ID != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setInt(2, excludePatientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking email uniqueness", e);
        }
        return false;
    }

    public boolean isPhoneTakenExcept(String phone, int excludePatientId) {
        if (phone == null || phone.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM PATIENTS WHERE PHONE = ? AND ID != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            ps.setInt(2, excludePatientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking phone uniqueness", e);
        }
        return false;
    }

    public List<Patient> getFilteredPatients(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        List<Patient> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + SELECT_COLUMNS + " FROM PATIENTS WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND CREATED_DATE >= ?");
            params.add(java.sql.Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND CREATED_DATE <= ?");
            params.add(java.sql.Timestamp.valueOf(toDate.atTime(23, 59, 59)));
        }
        sql.append(" ORDER BY ID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPatient(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving filtered patients", e);
        }
        return list;
    }

    private Patient mapRowToPatient(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("ID"));
        p.setName(rs.getString("NAME"));
        p.setAge(rs.getInt("AGE"));
        p.setUserId(rs.getInt("USER_ID"));
        p.setGender(rs.getString("GENDER"));
        p.setPhone(rs.getString("PHONE"));
        p.setEmail(rs.getString("EMAIL"));
        p.setAddress(rs.getString("ADDRESS"));
        try {
            p.setCreatedDate(rs.getTimestamp("CREATED_DATE"));
        } catch (SQLException ignored) {}
        return p;
    }
}

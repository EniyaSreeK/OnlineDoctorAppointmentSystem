package com.odas.dao;

import com.odas.Doctor;
import com.odas.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for DOCTORS table in Oracle 21c XE.
 * 
 * Purpose (for viva):
 * - Manages persistence operations for doctors (retrieving, adding, updating, and removing doctors).
 * - Stores professional qualifications, clinical experience, locations, and consultation availability.
 * - Employs PreparedStatement to ensure parameterized SQL execution.
 */
public class DoctorDAO {

    private static final Logger LOGGER = Logger.getLogger(DoctorDAO.class.getName());

    private static final String SELECT_COLUMNS = 
            "ID, NAME, SPECIALIZATION, USER_ID, QUALIFICATION, EXPERIENCE, PHONE, EMAIL, LOCATION, AVAILABLE_DAYS, AVAILABLE_TIME, CONSULTATION_FEE, CREATED_DATE";

    /**
     * Retrieves all doctors currently in the database.
     * 
     * @return List of Doctor entities
     */
    public List<Doctor> getAllDoctors() {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLUMNS + " FROM DOCTORS ORDER BY NAME ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToDoctor(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all doctors", e);
        }
        return list;
    }

    /**
     * Searches and filters doctors based on provided criteria:
     * - name: case-insensitive partial match
     * - specialization: case-insensitive partial match
     * - location: case-insensitive partial match
     * - availableDay: case-insensitive match (e.g. 'Mon', 'Wed', etc.)
     * 
     * Purpose (for viva):
     * - Employs parameterized PreparedStatement dynamically without string-concatenating user input.
     * - Protects against SQL injection vulnerabilities while enabling flexible multi-attribute querying.
     * 
     * @param name doctor name filter (optional)
     * @param specialization specialization filter (optional)
     * @param location location/clinic filter (optional)
     * @param availableDay available day filter (optional)
     * @return List of matching Doctor entities
     */
    public List<Doctor> searchDoctors(String name, String specialization, String location, String availableDay) {
        List<Doctor> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + SELECT_COLUMNS + " FROM DOCTORS WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (name != null && !name.trim().isEmpty()) {
            sql.append(" AND LOWER(NAME) LIKE ?");
            params.add("%" + name.trim().toLowerCase() + "%");
        }
        if (specialization != null && !specialization.trim().isEmpty()) {
            sql.append(" AND LOWER(SPECIALIZATION) LIKE ?");
            params.add("%" + specialization.trim().toLowerCase() + "%");
        }
        if (location != null && !location.trim().isEmpty()) {
            sql.append(" AND LOWER(LOCATION) LIKE ?");
            params.add("%" + location.trim().toLowerCase() + "%");
        }
        if (availableDay != null && !availableDay.trim().isEmpty()) {
            sql.append(" AND LOWER(AVAILABLE_DAYS) LIKE ?");
            params.add("%" + availableDay.trim().toLowerCase() + "%");
        }

        sql.append(" ORDER BY NAME ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDoctor(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching doctors with filters", e);
        }
        return list;
    }

    /**
     * Retrieves a single doctor by their primary key ID.
     * 
     * @param id doctor ID
     * @return Doctor entity or null if not found
     */
    public Doctor getDoctorById(int id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM DOCTORS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDoctor(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving doctor by id: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves a doctor associated with a specific USERS account ID.
     * 
     * @param userId user account ID
     * @return Doctor entity or null if not found
     */
    public Doctor getDoctorByUserId(int userId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM DOCTORS WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDoctor(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving doctor by userId: " + userId, e);
        }
        return null;
    }

    /**
     * Adds a new doctor to the database and sets the generated ID.
     * 
     * @param doctor Doctor entity to persist
     * @return generated ID or -1 on failure
     */
    public int addDoctor(Doctor doctor) {
        String sql = "INSERT INTO DOCTORS (NAME, SPECIALIZATION, USER_ID, QUALIFICATION, EXPERIENCE, " +
                     "PHONE, EMAIL, LOCATION, AVAILABLE_DAYS, AVAILABLE_TIME, CONSULTATION_FEE) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getSpecialization());
            if (doctor.getUserId() != null) {
                ps.setInt(3, doctor.getUserId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, doctor.getQualification());
            if (doctor.getExperience() != null) {
                ps.setInt(5, doctor.getExperience());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, doctor.getPhone());
            ps.setString(7, doctor.getEmail());
            ps.setString(8, doctor.getLocation());
            ps.setString(9, doctor.getAvailableDays());
            ps.setString(10, doctor.getAvailableTime());
            ps.setDouble(11, doctor.getConsultationFee() != null ? doctor.getConsultationFee() : 500.0);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        doctor.setId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding doctor: " + doctor.getName(), e);
        }
        return -1;
    }

    /**
     * Updates an existing doctor's full profile information.
     * 
     * @param doctor Doctor entity with updated information
     * @return true if updated successfully, false otherwise
     */
    public boolean updateDoctor(Doctor doctor) {
        String sql = "UPDATE DOCTORS SET NAME = ?, SPECIALIZATION = ?, QUALIFICATION = ?, EXPERIENCE = ?, " +
                     "PHONE = ?, EMAIL = ?, LOCATION = ?, AVAILABLE_DAYS = ?, AVAILABLE_TIME = ?, CONSULTATION_FEE = ? " +
                     "WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getSpecialization());
            ps.setString(3, doctor.getQualification());
            if (doctor.getExperience() != null) {
                ps.setInt(4, doctor.getExperience());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, doctor.getPhone());
            ps.setString(6, doctor.getEmail());
            ps.setString(7, doctor.getLocation());
            ps.setString(8, doctor.getAvailableDays());
            ps.setString(9, doctor.getAvailableTime());
            ps.setDouble(10, doctor.getConsultationFee() != null ? doctor.getConsultationFee() : 500.0);
            ps.setInt(11, doctor.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating doctor id: " + doctor.getId(), e);
        }
        return false;
    }

    /**
     * Updates only a doctor's availability schedule (available days and time range).
     *
     * @param doctorId doctor ID
     * @param availableDays comma-separated days (e.g. 'Mon,Wed,Fri')
     * @param availableTime 24-hr time range (e.g. '09:00-13:00')
     * @return true if updated successfully, false otherwise
     */
    public boolean updateDoctorSchedule(int doctorId, String availableDays, String availableTime) {
        String sql = "UPDATE DOCTORS SET AVAILABLE_DAYS = ?, AVAILABLE_TIME = ? WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, availableDays != null ? availableDays.trim() : null);
            ps.setString(2, availableTime != null ? availableTime.trim() : null);
            ps.setInt(3, doctorId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating schedule for doctor id: " + doctorId, e);
        }
        return false;
    }

    /**
     * Deletes a doctor from the database by ID.
     * 
     * @param id doctor ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteDoctor(int id) {
        String sql = "DELETE FROM DOCTORS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) {
                LOGGER.log(Level.WARNING, "Cannot delete doctor id " + id + " due to active foreign key references (prescriptions/payments/appointments).");
            } else {
                LOGGER.log(Level.SEVERE, "Error deleting doctor id: " + id, e);
            }
        }
        return false;
    }

    public List<Doctor> getFilteredDoctors(java.time.LocalDate fromDate, java.time.LocalDate toDate, String specialization) {
        List<Doctor> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + SELECT_COLUMNS + " FROM DOCTORS WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND CREATED_DATE >= ?");
            params.add(java.sql.Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND CREATED_DATE <= ?");
            params.add(java.sql.Timestamp.valueOf(toDate.atTime(23, 59, 59)));
        }
        if (specialization != null && !specialization.trim().isEmpty() && !"ALL".equalsIgnoreCase(specialization.trim())) {
            sql.append(" AND LOWER(SPECIALIZATION) LIKE ?");
            params.add("%" + specialization.trim().toLowerCase() + "%");
        }
        sql.append(" ORDER BY ID ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDoctor(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving filtered doctors", e);
        }
        return list;
    }

    private Doctor mapRowToDoctor(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("ID"));
        d.setName(rs.getString("NAME"));
        d.setSpecialization(rs.getString("SPECIALIZATION"));
        int userId = rs.getInt("USER_ID");
        if (!rs.wasNull()) {
            d.setUserId(userId);
        }
        d.setQualification(rs.getString("QUALIFICATION"));
        int exp = rs.getInt("EXPERIENCE");
        if (!rs.wasNull()) {
            d.setExperience(exp);
        }
        d.setPhone(rs.getString("PHONE"));
        d.setEmail(rs.getString("EMAIL"));
        d.setLocation(rs.getString("LOCATION"));
        d.setAvailableDays(rs.getString("AVAILABLE_DAYS"));
        d.setAvailableTime(rs.getString("AVAILABLE_TIME"));
        double fee = rs.getDouble("CONSULTATION_FEE");
        if (!rs.wasNull()) {
            d.setConsultationFee(fee);
        } else {
            d.setConsultationFee(500.0);
        }
        try {
            d.setCreatedDate(rs.getTimestamp("CREATED_DATE"));
        } catch (SQLException ignored) {}
        return d;
    }
}

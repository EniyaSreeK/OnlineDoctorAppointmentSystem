package com.odas.dao;

import com.odas.Appointment;
import com.odas.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for APPOINTMENTS table in Oracle 21c XE.
 * 
 * Purpose (for viva):
 * - Encapsulates appointment booking, scheduling conflict checks, and status lifecycle.
 * - Prevents double-booking via application-level verification on (doctor + date + time) and database UNIQUE constraint.
 * - Joins APPOINTMENTS with DOCTORS and PATIENTS to produce enriched records for dashboards.
 */
public class AppointmentDAO {

    private static final Logger LOGGER = Logger.getLogger(AppointmentDAO.class.getName());

    private static final String SELECT_APPT_COLUMNS = 
            "a.ID, a.DOCTOR_ID, a.PATIENT_ID, a.APPOINTMENT_DATE, a.STATUS, a.APPOINTMENT_TIME, a.PAYMENT_STATUS, a.REMINDER_SENT, " +
            "d.NAME AS DOCTOR_NAME, d.SPECIALIZATION AS DOCTOR_SPEC, p.NAME AS PATIENT_NAME ";

    /**
     * Checks if a doctor is already booked for a specific date and time slot.
     * 
     * @param doctorId doctor ID
     * @param appointmentDate date of appointment
     * @param appointmentTime time slot string (e.g. '10:00 AM')
     * @return true if slot is already occupied, false otherwise
     */
    public boolean isSlotBooked(int doctorId, Timestamp appointmentDate, String appointmentTime) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS " +
                     "WHERE DOCTOR_ID = ? AND TRUNC(APPOINTMENT_DATE) = TRUNC(?) " +
                     "AND APPOINTMENT_TIME = ? AND STATUS <> 'CANCELLED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            ps.setTimestamp(2, appointmentDate);
            ps.setString(3, appointmentTime != null ? appointmentTime : "10:00");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking appointment slot availability for time slot. Failing closed.", e);
            return true; // Fail-closed: treat as booked to prevent double-booking on DB error
        }
        return false;
    }

    /**
     * Legacy slot check comparing by doctor and exact date/time.
     * 
     * @param doctorId doctor ID
     * @param appointmentDate date and time of appointment
     * @return true if slot is already occupied, false otherwise
     */
    public boolean isSlotBooked(int doctorId, Timestamp appointmentDate) {
        return isSlotBooked(doctorId, appointmentDate, "10:00");
    }

    /**
     * Checks if a doctor's slot is booked on a date and time, excluding a specific appointment ID.
     * Used during appointment rescheduling to prevent self-collision while forbidding double booking.
     * 
     * @param doctorId doctor ID
     * @param appointmentDate appointment date
     * @param appointmentTime time slot string (e.g. '10:00')
     * @param excludeAppointmentId current appointment ID being rescheduled
     * @return true if slot is occupied by another appointment, false otherwise
     */
    public boolean isSlotBookedExcept(int doctorId, Timestamp appointmentDate, String appointmentTime, int excludeAppointmentId) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS " +
                     "WHERE DOCTOR_ID = ? AND TRUNC(APPOINTMENT_DATE) = TRUNC(?) " +
                     "AND APPOINTMENT_TIME = ? AND STATUS <> 'CANCELLED' AND ID <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            ps.setTimestamp(2, appointmentDate);
            ps.setString(3, appointmentTime != null ? appointmentTime : "10:00");
            ps.setInt(4, excludeAppointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking appointment slot conflict for reschedule. Failing closed.", e);
            return true; // Fail-closed: treat as booked to prevent double-booking on DB error
        }
        return false;
    }

    /**
     * Checks if a patient already has an active appointment for a specific date and time slot across any doctor.
     * Enforces the SRS constraint (Section 8): "Patients cannot book multiple appointments for the same time slot."
     * 
     * @param patientId patient ID
     * @param appointmentDate date of appointment
     * @param appointmentTime time slot string (e.g. '10:00')
     * @return true if patient already has a slot occupied, false otherwise
     */
    public boolean isPatientSlotBooked(int patientId, Timestamp appointmentDate, String appointmentTime) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS " +
                     "WHERE PATIENT_ID = ? AND TRUNC(APPOINTMENT_DATE) = TRUNC(?) " +
                     "AND APPOINTMENT_TIME = ? AND STATUS <> 'CANCELLED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            ps.setTimestamp(2, appointmentDate);
            ps.setString(3, appointmentTime != null ? appointmentTime : "10:00");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking patient slot availability. Failing closed.", e);
            return true; // Fail-closed: treat as booked to prevent double-booking on DB error
        }
        return false;
    }

    /**
     * Checks if a patient has an active appointment on a date and time, excluding a specific appointment ID.
     * Used during appointment rescheduling to prevent self-collision while forbidding patient double booking.
     * 
     * @param patientId patient ID
     * @param appointmentDate appointment date
     * @param appointmentTime time slot string (e.g. '10:00')
     * @param excludeAppointmentId current appointment ID being rescheduled
     * @return true if slot is occupied by another appointment of this patient, false otherwise
     */
    public boolean isPatientSlotBookedExcept(int patientId, Timestamp appointmentDate, String appointmentTime, int excludeAppointmentId) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS " +
                     "WHERE PATIENT_ID = ? AND TRUNC(APPOINTMENT_DATE) = TRUNC(?) " +
                     "AND APPOINTMENT_TIME = ? AND STATUS <> 'CANCELLED' AND ID <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            ps.setTimestamp(2, appointmentDate);
            ps.setString(3, appointmentTime != null ? appointmentTime : "10:00");
            ps.setInt(4, excludeAppointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking patient slot conflict for reschedule. Failing closed.", e);
            return true; // Fail-closed: treat as booked to prevent double-booking on DB error
        }
        return false;
    }

    /**
     * Reschedules an existing scheduled appointment to a new date and time slot in place.
     * Updates the existing record without creating duplicate rows.
     * 
     * @param appointmentId existing appointment ID
     * @param newDate new appointment date timestamp
     * @param newTime new time slot string (e.g. '11:00')
     * @return true if successfully rescheduled, false otherwise
     */
    public boolean rescheduleAppointment(int appointmentId, Timestamp newDate, String newTime) {
        String sql = "UPDATE APPOINTMENTS SET APPOINTMENT_DATE = ?, APPOINTMENT_TIME = ?, REMINDER_SENT = 0 " +
                     "WHERE ID = ? AND STATUS = 'SCHEDULED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, newDate);
            ps.setString(2, newTime != null ? newTime : "10:00");
            ps.setInt(3, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                LOGGER.log(Level.WARNING, "Slot already booked (ORA-00001) while rescheduling appointment ID: " + appointmentId);
            } else {
                LOGGER.log(Level.SEVERE, "Error rescheduling appointment ID: " + appointmentId, e);
            }
        }
        return false;
    }

    /**
     * Books a new appointment and returns the generated ID.
     * Returns -2 if unique slot constraint is violated (ORA-00001), or -1 on other failure.
     * 
     * @param appointment Appointment entity
     * @return generated ID, -2 for duplicate slot, or -1 on failure
     */
    public int bookAppointment(Appointment appointment) {
        String sql = "INSERT INTO APPOINTMENTS (DOCTOR_ID, PATIENT_ID, APPOINTMENT_DATE, STATUS, APPOINTMENT_TIME, PAYMENT_STATUS) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            ps.setInt(1, appointment.getDoctorId());
            ps.setInt(2, appointment.getPatientId());
            ps.setTimestamp(3, appointment.getAppointmentDate());
            ps.setString(4, appointment.getStatus() != null ? appointment.getStatus() : "SCHEDULED");
            ps.setString(5, appointment.getAppointmentTime() != null ? appointment.getAppointmentTime() : "10:00");
            ps.setString(6, appointment.getPaymentStatus() != null ? appointment.getPaymentStatus() : "UNPAID");

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        appointment.setId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                LOGGER.log(Level.WARNING, "Slot already booked (ORA-00001) for doctor " + appointment.getDoctorId());
                return -2;
            }
            LOGGER.log(Level.SEVERE, "Error booking appointment for patient: " + appointment.getPatientId(), e);
        }
        return -1;
    }

    /**
     * Retrieves an appointment by ID with joined doctor and patient details.
     * 
     * @param id appointment ID
     * @return Appointment entity or null
     */
    public Appointment getAppointmentById(int id) {
        String sql = "SELECT " + SELECT_APPT_COLUMNS +
                     "FROM APPOINTMENTS a " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS p ON a.PATIENT_ID = p.ID " +
                     "WHERE a.ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAppointment(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving appointment by id: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves all appointments for a given patient.
     * 
     * @param patientId patient ID
     * @return List of Appointment entities
     */
    public List<Appointment> getAppointmentsByPatientId(int patientId) {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_APPT_COLUMNS +
                     "FROM APPOINTMENTS a " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS p ON a.PATIENT_ID = p.ID " +
                     "WHERE a.PATIENT_ID = ? " +
                     "ORDER BY a.APPOINTMENT_DATE DESC, a.ID DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving appointments for patientId: " + patientId, e);
        }
        return list;
    }

    /**
     * Retrieves all appointments for a given doctor.
     * 
     * @param doctorId doctor ID
     * @return List of Appointment entities
     */
    public List<Appointment> getAppointmentsByDoctorId(int doctorId) {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_APPT_COLUMNS +
                     "FROM APPOINTMENTS a " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS p ON a.PATIENT_ID = p.ID " +
                     "WHERE a.DOCTOR_ID = ? " +
                     "ORDER BY a.APPOINTMENT_DATE ASC, a.ID ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving appointments for doctorId: " + doctorId, e);
        }
        return list;
    }

    /**
     * Retrieves all appointments in the system (for admin dashboard).
     * 
     * @return List of Appointment entities
     */
    public List<Appointment> getAllAppointments() {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_APPT_COLUMNS +
                     "FROM APPOINTMENTS a " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS p ON a.PATIENT_ID = p.ID " +
                     "ORDER BY a.APPOINTMENT_DATE DESC, a.ID DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToAppointment(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all appointments", e);
        }
        return list;
    }

    /**
     * Updates an appointment's status (e.g., SCHEDULED, COMPLETED, CANCELLED).
     * 
     * @param appointmentId appointment ID
     * @param status new status string
     * @return true if updated, false otherwise
     */
    public boolean updateStatus(int appointmentId, String status) {
        String sql = "UPDATE APPOINTMENTS SET STATUS = ? WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating status for appointment: " + appointmentId, e);
        }
        return false;
    }

    /**
     * Updates an appointment's payment status (e.g., UNPAID, PAID).
     * 
     * @param appointmentId appointment ID
     * @param paymentStatus new payment status string
     * @return true if updated, false otherwise
     */
    public boolean updatePaymentStatus(int appointmentId, String paymentStatus) {
        String sql = "UPDATE APPOINTMENTS SET PAYMENT_STATUS = ? WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, paymentStatus);
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating payment status for appointment: " + appointmentId, e);
        }
        return false;
    }

    /**
     * Cancels an appointment. If the appointment was PAID, marks payment status as REFUNDED
     * across APPOINTMENTS and PAYMENTS within an atomic transaction.
     * 
     * @param appointmentId appointment ID
     * @return true if cancelled, false otherwise
     */
    public boolean cancelAppointment(int appointmentId) {
        Connection conn = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdateAppt = null;
        PreparedStatement psUpdatePay = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Step 1: Check current payment status with row lock
            psSelect = conn.prepareStatement("SELECT PAYMENT_STATUS FROM APPOINTMENTS WHERE ID = ? FOR UPDATE");
            psSelect.setInt(1, appointmentId);
            rs = psSelect.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            String paymentStatus = rs.getString("PAYMENT_STATUS");

            if ("PAID".equalsIgnoreCase(paymentStatus)) {
                // If paid, cancel and mark REFUNDED in both tables atomically
                psUpdateAppt = conn.prepareStatement("UPDATE APPOINTMENTS SET STATUS = 'CANCELLED', PAYMENT_STATUS = 'REFUNDED' WHERE ID = ?");
                psUpdateAppt.setInt(1, appointmentId);
                psUpdateAppt.executeUpdate();

                psUpdatePay = conn.prepareStatement("UPDATE PAYMENTS SET PAYMENT_STATUS = 'REFUNDED' WHERE APPOINTMENT_ID = ?");
                psUpdatePay.setInt(1, appointmentId);
                psUpdatePay.executeUpdate();
            } else {
                psUpdateAppt = conn.prepareStatement("UPDATE APPOINTMENTS SET STATUS = 'CANCELLED' WHERE ID = ?");
                psUpdateAppt.setInt(1, appointmentId);
                psUpdateAppt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error cancelling appointment ID: " + appointmentId, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (psSelect != null) try { psSelect.close(); } catch (SQLException ignored) {}
            if (psUpdateAppt != null) try { psUpdateAppt.close(); } catch (SQLException ignored) {}
            if (psUpdatePay != null) try { psUpdatePay.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Deletes an appointment record by ID.
     * Safely handles foreign key dependencies (prescriptions/payments).
     * 
     * @param appointmentId appointment ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteAppointment(int appointmentId) {
        String sql = "DELETE FROM APPOINTMENTS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) {
                LOGGER.log(Level.WARNING, "Cannot delete appointment id " + appointmentId + " due to active foreign key references.");
            } else {
                LOGGER.log(Level.SEVERE, "Error deleting appointment id: " + appointmentId, e);
            }
        }
        return false;
    }

    /**
     * Retrieves filtered appointments for Admin reporting by optional date range and status.
     *
     * @param fromDate start date (inclusive), or null for unbounded
     * @param toDate end date (inclusive), or null for unbounded
     * @param status status filter ('ALL', 'SCHEDULED', 'COMPLETED', 'CANCELLED'), or null/empty for all
     * @return List of matching Appointment entities
     */
    public List<Appointment> getFilteredAppointments(LocalDate fromDate, LocalDate toDate, String status) {
        List<Appointment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(SELECT_APPT_COLUMNS)
           .append("FROM APPOINTMENTS a ")
           .append("JOIN DOCTORS d ON a.DOCTOR_ID = d.ID ")
           .append("JOIN PATIENTS p ON a.PATIENT_ID = p.ID ")
           .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (fromDate != null) {
            sql.append("AND TRUNC(a.APPOINTMENT_DATE) >= ? ");
            params.add(java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append("AND TRUNC(a.APPOINTMENT_DATE) <= ? ");
            params.add(java.sql.Date.valueOf(toDate));
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append("AND a.STATUS = ? ");
            params.add(status.trim().toUpperCase());
        }
        sql.append("ORDER BY a.APPOINTMENT_DATE DESC, a.ID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof java.sql.Date) {
                    ps.setDate(i + 1, (java.sql.Date) param);
                } else if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving filtered appointments for reports", e);
        }
        return list;
    }

    /**
     * Retrieves SCHEDULED appointments with REMINDER_SENT = 0 whose TRUNC(APPOINTMENT_DATE)
     * is between TRUNC(today) and TRUNC(today + 1 day).
     *
     * @return List of matching Appointment entities
     */
    public List<Appointment> getUpcomingUnreminded() {
        return getUpcomingUnreminded(LocalDate.now());
    }

    /**
     * Overload for backward compatibility.
     *
     * @param withinHours hours window (delegates to today's date lookup)
     * @return List of matching Appointment entities
     */
    public List<Appointment> getUpcomingUnreminded(int withinHours) {
        return getUpcomingUnreminded(LocalDate.now());
    }

    /**
     * Retrieves SCHEDULED appointments with REMINDER_SENT = 0 whose TRUNC(APPOINTMENT_DATE)
     * is between TRUNC(today) and TRUNC(today + 1 day) (parameterized, using java.sql.Date).
     *
     * @param today reference date (today)
     * @return List of matching Appointment entities
     */
    public List<Appointment> getUpcomingUnreminded(LocalDate today) {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_APPT_COLUMNS +
                     "FROM APPOINTMENTS a " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "JOIN PATIENTS p ON a.PATIENT_ID = p.ID " +
                     "WHERE a.STATUS = 'SCHEDULED' " +
                     "AND a.REMINDER_SENT = 0 " +
                     "AND TRUNC(a.APPOINTMENT_DATE) >= ? " +
                     "AND TRUNC(a.APPOINTMENT_DATE) <= ? " +
                     "ORDER BY a.APPOINTMENT_DATE ASC, a.ID ASC";

        LocalDate refDate = (today != null) ? today : LocalDate.now();
        java.sql.Date sqlToday = java.sql.Date.valueOf(refDate);
        java.sql.Date sqlTomorrow = java.sql.Date.valueOf(refDate.plusDays(1));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, sqlToday);
            ps.setDate(2, sqlTomorrow);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving upcoming unreminded appointments", e);
        }
        return list;
    }

    /**
     * Marks an appointment's reminder as sent (REMINDER_SENT = 1).
     *
     * @param id appointment ID
     * @return true if updated, false otherwise
     */
    public boolean markReminderSent(int id) {
        String sql = "UPDATE APPOINTMENTS SET REMINDER_SENT = 1 WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marking reminder sent for appointment ID: " + id, e);
        }
        return false;
    }

    private Appointment mapRowToAppointment(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("ID"));
        a.setDoctorId(rs.getInt("DOCTOR_ID"));
        a.setPatientId(rs.getInt("PATIENT_ID"));
        a.setAppointmentDate(rs.getTimestamp("APPOINTMENT_DATE"));
        a.setStatus(rs.getString("STATUS"));
        a.setAppointmentTime(rs.getString("APPOINTMENT_TIME"));
        a.setPaymentStatus(rs.getString("PAYMENT_STATUS"));
        try {
            a.setReminderSent(rs.getInt("REMINDER_SENT"));
        } catch (SQLException ignored) {
            // Column may be absent if custom queries without REMINDER_SENT are ever mapped
        }
        a.setDoctorName(rs.getString("DOCTOR_NAME"));
        a.setDoctorSpecialization(rs.getString("DOCTOR_SPEC"));
        a.setPatientName(rs.getString("PATIENT_NAME"));
        return a;
    }
}

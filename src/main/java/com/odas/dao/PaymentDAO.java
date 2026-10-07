package com.odas.dao;

import com.odas.Payment;
import com.odas.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for PAYMENTS table in Oracle 21c XE.
 * 
 * Purpose (for viva):
 * - Manages consultation payment transactions and receipt generation.
 * - Supports simulated online payment workflows (Card, UPI, Net Banking).
 * - Enforces parameter binding via PreparedStatement to avoid SQL injection vulnerabilities.
 */
public class PaymentDAO {

    private static final Logger LOGGER = Logger.getLogger(PaymentDAO.class.getName());

    private static final String SELECT_PAYMENT_COLUMNS = 
            "pay.ID, pay.APPOINTMENT_ID, pay.PATIENT_ID, pay.AMOUNT, pay.PAYMENT_MODE, pay.PAYMENT_STATUS, pay.PAYMENT_DATE, " +
            "pt.NAME AS PATIENT_NAME, d.NAME AS DOCTOR_NAME, d.SPECIALIZATION AS DOCTOR_SPEC, a.APPOINTMENT_DATE, a.APPOINTMENT_TIME, a.STATUS AS APPT_STATUS ";

    /**
     * Records a new payment transaction and returns the generated primary key.
     * 
     * @param payment Payment entity
     * @return generated ID or -1 on failure
     */
    public int addPayment(Payment payment) {
        String sql = "INSERT INTO PAYMENTS (APPOINTMENT_ID, PATIENT_ID, AMOUNT, PAYMENT_MODE, PAYMENT_STATUS) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            ps.setInt(1, payment.getAppointmentId());
            ps.setInt(2, payment.getPatientId());
            ps.setDouble(3, payment.getAmount());
            ps.setString(4, payment.getPaymentMode());
            ps.setString(5, payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "SUCCESS");

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        payment.setId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error recording payment for appointment: " + payment.getAppointmentId(), e);
        }
        return -1;
    }

    /**
     * Retrieves a payment by primary key ID.
     * 
     * @param id payment ID
     * @return Payment entity or null
     */
    public Payment getPaymentById(int id) {
        String sql = "SELECT " + SELECT_PAYMENT_COLUMNS +
                     "FROM PAYMENTS pay " +
                     "JOIN APPOINTMENTS a ON pay.APPOINTMENT_ID = a.ID " +
                     "JOIN PATIENTS pt ON pay.PATIENT_ID = pt.ID " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "WHERE pay.ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPayment(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payment by id: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves the payment record for a specific appointment.
     * 
     * @param appointmentId appointment ID
     * @return Payment entity or null if unpaid
     */
    public Payment getPaymentByAppointmentId(int appointmentId) {
        String sql = "SELECT " + SELECT_PAYMENT_COLUMNS +
                     "FROM PAYMENTS pay " +
                     "JOIN APPOINTMENTS a ON pay.APPOINTMENT_ID = a.ID " +
                     "JOIN PATIENTS pt ON pay.PATIENT_ID = pt.ID " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "WHERE pay.APPOINTMENT_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPayment(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payment by appointmentId: " + appointmentId, e);
        }
        return null;
    }

    /**
     * Retrieves all payment transactions for a given patient.
     * 
     * @param patientId patient ID
     * @return List of Payment entities
     */
    public List<Payment> getPaymentsByPatientId(int patientId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_PAYMENT_COLUMNS +
                     "FROM PAYMENTS pay " +
                     "JOIN APPOINTMENTS a ON pay.APPOINTMENT_ID = a.ID " +
                     "JOIN PATIENTS pt ON pay.PATIENT_ID = pt.ID " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "WHERE pay.PATIENT_ID = ? " +
                     "ORDER BY pay.PAYMENT_DATE DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPayment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payments for patientId: " + patientId, e);
        }
        return list;
    }

    /**
     * Retrieves all payment transactions across the system (for admin reports).
     * 
     * @return List of Payment entities
     */
    public List<Payment> getAllPayments() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_PAYMENT_COLUMNS +
                     "FROM PAYMENTS pay " +
                     "JOIN APPOINTMENTS a ON pay.APPOINTMENT_ID = a.ID " +
                     "JOIN PATIENTS pt ON pay.PATIENT_ID = pt.ID " +
                     "JOIN DOCTORS d ON a.DOCTOR_ID = d.ID " +
                     "ORDER BY pay.PAYMENT_DATE DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToPayment(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all payments", e);
        }
        return list;
    }

    /**
     * Calculates the sum of all successful payments.
     * 
     * @return total revenue
     */
    public double getTotalRevenue() {
        String sql = "SELECT NVL(SUM(AMOUNT), 0) FROM PAYMENTS WHERE PAYMENT_STATUS = 'SUCCESS'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total revenue", e);
        }
        return 0.0;
    }

    /**
     * Executes atomic consultation fee payment in a single database transaction.
     * Inserts into PAYMENTS (status SUCCESS) and updates APPOINTMENTS.PAYMENT_STATUS to 'PAID'.
     * Rolls back both if either step fails.
     *
     * @param payment Payment entity containing appointmentId, patientId, amount, paymentMode
     * @return generated payment ID, or -1 on transaction failure/rollback
     */
    public int processPaymentTransaction(Payment payment) {
        String insertSql = "INSERT INTO PAYMENTS (APPOINTMENT_ID, PATIENT_ID, AMOUNT, PAYMENT_MODE, PAYMENT_STATUS) " +
                           "VALUES (?, ?, ?, ?, 'SUCCESS')";
        String updateApptSql = "UPDATE APPOINTMENTS SET PAYMENT_STATUS = 'PAID' WHERE ID = ?";

        Connection conn = null;
        PreparedStatement psInsert = null;
        PreparedStatement psUpdate = null;
        ResultSet rsKeys = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin atomic transaction

            // Step 1: Lock the appointment row and re-check PAYMENT_STATUS and STATUS
            String lockSql = "SELECT STATUS, PAYMENT_STATUS FROM APPOINTMENTS WHERE ID = ? FOR UPDATE";
            try (PreparedStatement psLock = conn.prepareStatement(lockSql)) {
                psLock.setInt(1, payment.getAppointmentId());
                try (ResultSet rsLock = psLock.executeQuery()) {
                    if (!rsLock.next()) {
                        LOGGER.warning("Payment failed: Appointment #" + payment.getAppointmentId() + " not found.");
                        conn.rollback();
                        return -1;
                    }
                    String status = rsLock.getString("STATUS");
                    String paymentStatus = rsLock.getString("PAYMENT_STATUS");

                    // Must be UNPAID, and STATUS must be SCHEDULED or COMPLETED
                    if (!"UNPAID".equalsIgnoreCase(paymentStatus)) {
                        LOGGER.warning("Payment failed: Appointment #" + payment.getAppointmentId() + " is already " + paymentStatus);
                        conn.rollback();
                        return -1;
                    }
                    if (!"SCHEDULED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(status)) {
                        LOGGER.warning("Payment failed: Appointment #" + payment.getAppointmentId() + " status is " + status);
                        conn.rollback();
                        return -1;
                    }
                }
            }

            // Step 2: Insert payment record
            psInsert = conn.prepareStatement(insertSql, new String[]{"ID"});
            psInsert.setInt(1, payment.getAppointmentId());
            psInsert.setInt(2, payment.getPatientId());
            psInsert.setDouble(3, payment.getAmount());
            psInsert.setString(4, payment.getPaymentMode());

            int affected = psInsert.executeUpdate();
            if (affected <= 0) {
                conn.rollback();
                return -1;
            }

            int paymentId = -1;
            rsKeys = psInsert.getGeneratedKeys();
            if (rsKeys != null && rsKeys.next()) {
                paymentId = rsKeys.getInt(1);
                payment.setId(paymentId);
            } else {
                conn.rollback();
                return -1;
            }

            // Step 2: Update appointment payment status to PAID
            psUpdate = conn.prepareStatement(updateApptSql);
            psUpdate.setInt(1, payment.getAppointmentId());
            int apptAffected = psUpdate.executeUpdate();
            if (apptAffected <= 0) {
                conn.rollback();
                return -1;
            }

            // Step 3: Commit atomic transaction
            conn.commit();
            LOGGER.info("Payment transaction successfully committed: Payment ID=" + paymentId + 
                        " for Appointment #" + payment.getAppointmentId() + " (Amount=" + payment.getAmount() + ")");
            return paymentId;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in processPaymentTransaction for appointment #" + 
                       payment.getAppointmentId() + ". Transaction rolling back.", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failure", ex);
                }
            }
            return -1;
        } finally {
            if (rsKeys != null) try { rsKeys.close(); } catch (SQLException ignored) {}
            if (psInsert != null) try { psInsert.close(); } catch (SQLException ignored) {}
            if (psUpdate != null) try { psUpdate.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Deletes a payment record by ID (used for test cleanup and maintenance).
     *
     * @param id payment ID
     * @return true if deleted, false otherwise
     */
    public boolean deletePayment(int id) {
        String sql = "DELETE FROM PAYMENTS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting payment id: " + id, e);
        }
        return false;
    }

    /**
     * Retrieves filtered payments for Admin reporting by optional date range and payment mode.
     *
     * @param fromDate start date (inclusive), or null for unbounded
     * @param toDate end date (inclusive), or null for unbounded
     * @param mode optional payment mode ('ALL', 'Card', 'UPI', 'Net Banking')
     * @return List of matching Payment entities
     */
    public List<Payment> getFilteredPayments(LocalDate fromDate, LocalDate toDate, String mode) {
        List<Payment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(SELECT_PAYMENT_COLUMNS)
           .append("FROM PAYMENTS pay ")
           .append("JOIN APPOINTMENTS a ON pay.APPOINTMENT_ID = a.ID ")
           .append("JOIN PATIENTS pt ON pay.PATIENT_ID = pt.ID ")
           .append("JOIN DOCTORS d ON a.DOCTOR_ID = d.ID ")
           .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (fromDate != null) {
            sql.append("AND TRUNC(pay.PAYMENT_DATE) >= ? ");
            params.add(java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append("AND TRUNC(pay.PAYMENT_DATE) <= ? ");
            params.add(java.sql.Date.valueOf(toDate));
        }
        if (mode != null && !mode.trim().isEmpty() && !"ALL".equalsIgnoreCase(mode.trim())) {
            sql.append("AND pay.PAYMENT_MODE = ? ");
            params.add(mode.trim());
        }
        sql.append("ORDER BY pay.PAYMENT_DATE DESC, pay.ID DESC");

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
                    list.add(mapRowToPayment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving filtered payments for reports", e);
        }
        return list;
    }

    private Payment mapRowToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("ID"));
        p.setAppointmentId(rs.getInt("APPOINTMENT_ID"));
        p.setPatientId(rs.getInt("PATIENT_ID"));
        p.setAmount(rs.getDouble("AMOUNT"));
        p.setPaymentMode(rs.getString("PAYMENT_MODE"));
        p.setPaymentStatus(rs.getString("PAYMENT_STATUS"));
        p.setPaymentDate(rs.getTimestamp("PAYMENT_DATE"));
        p.setPatientName(rs.getString("PATIENT_NAME"));
        p.setDoctorName(rs.getString("DOCTOR_NAME"));
        p.setDoctorSpecialization(rs.getString("DOCTOR_SPEC"));
        p.setAppointmentDate(rs.getTimestamp("APPOINTMENT_DATE"));
        try {
            p.setAppointmentTime(rs.getString("APPOINTMENT_TIME"));
            p.setAppointmentStatus(rs.getString("APPT_STATUS"));
        } catch (SQLException ignored) {}
        return p;
    }
}

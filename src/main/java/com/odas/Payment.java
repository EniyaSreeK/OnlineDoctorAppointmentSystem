package com.odas;

import java.sql.Timestamp;

/**
 * Payment entity representing consultation fee transactions.
 * 
 * Purpose (for viva):
 * - Maps to the PAYMENTS table in Oracle 21c XE.
 * - Tracks appointment payment status (SUCCESS, FAILED, PENDING) and payment mode.
 * - Supports simulated online payment receipts and financial reporting.
 */
public class Payment {

    private int id;
    private int appointmentId;
    private int patientId;
    private double amount;
    private String paymentMode;
    private String paymentStatus = "SUCCESS";
    private Timestamp paymentDate;

    // Enriched fields for receipt and reporting display
    private String patientName;
    private String doctorName;
    private String doctorSpecialization;
    private Timestamp appointmentDate;
    private String appointmentTime;
    private String appointmentStatus;

    public Payment() {
    }

    public Payment(int appointmentId, int patientId, double amount, String paymentMode, String paymentStatus) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.paymentStatus = paymentStatus;
    }

    public Payment(int id, int appointmentId, int patientId, double amount, 
                   String paymentMode, String paymentStatus, Timestamp paymentDate) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Timestamp getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Timestamp paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorSpecialization() {
        return doctorSpecialization;
    }

    public void setDoctorSpecialization(String doctorSpecialization) {
        this.doctorSpecialization = doctorSpecialization;
    }

    public Timestamp getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Timestamp appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getFormattedTime() {
        return com.odas.util.TimeSlotUtil.formatSlotLabel(appointmentTime);
    }

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(String appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", appointmentId=" + appointmentId +
                ", patientId=" + patientId +
                ", amount=" + amount +
                ", paymentMode='" + paymentMode + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentDate=" + paymentDate +
                '}';
    }
}

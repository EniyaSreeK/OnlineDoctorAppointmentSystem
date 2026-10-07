package com.odas;

import java.sql.Timestamp;

/**
 * Appointment entity representing doctor-patient bookings.
 * 
 * Purpose (for viva):
 * - Maps to the APPOINTMENTS table in Oracle.
 * - Stores appointment schedule (date + time slot), status, payment status, and doctor/patient foreign keys.
 * - Enforces business rules like scheduling status (SCHEDULED, COMPLETED, CANCELLED) and payment status.
 */
public class Appointment {

    private int appointmentId;
    private int doctorId;
    private int patientId;
    private Timestamp appointmentDate;
    private String date; // Formatted date string for display
    private String status = "SCHEDULED";
    private String appointmentTime = "10:00";
    private String paymentStatus = "UNPAID";
    private int reminderSent = 0; // 0 = not sent, 1 = reminder sent

    // Related objects & display fields
    private Doctor doctor;
    private Patient patient;
    private String doctorName;
    private String doctorSpecialization;
    private String patientName;

    public Appointment() {
    }

    // Existing constructor preserved for backward compatibility
    public Appointment(int appointmentId, Doctor doctor, Patient patient, String date) {
        this.appointmentId = appointmentId;
        this.doctor = doctor;
        this.patient = patient;
        this.date = date;
        if (doctor != null) {
            this.doctorId = doctor.getDoctorId();
            this.doctorName = doctor.getDoctorName();
            this.doctorSpecialization = doctor.getSpecialization();
        }
        if (patient != null) {
            this.patientId = patient.getPatientId();
            this.patientName = patient.getPatientName();
        }
    }

    public Appointment(int doctorId, int patientId, Timestamp appointmentDate, String status) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }

    public Appointment(int doctorId, int patientId, Timestamp appointmentDate, String status, String appointmentTime) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.appointmentDate = appointmentDate;
        this.status = status;
        this.appointmentTime = appointmentTime;
    }

    public Appointment(int doctorId, int patientId, Timestamp appointmentDate, 
                       String status, String appointmentTime, String paymentStatus) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.appointmentDate = appointmentDate;
        this.status = status;
        this.appointmentTime = appointmentTime;
        this.paymentStatus = paymentStatus;
    }

    public Appointment(int appointmentId, int doctorId, int patientId, Timestamp appointmentDate, String status) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }

    public Appointment(int appointmentId, int doctorId, int patientId, Timestamp appointmentDate, 
                       String status, String appointmentTime, String paymentStatus) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.appointmentDate = appointmentDate;
        this.status = status;
        this.appointmentTime = appointmentTime;
        this.paymentStatus = paymentStatus;
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    // Convenience alias for getId()
    public int getId() {
        return appointmentId;
    }

    public void setId(int id) {
        this.appointmentId = id;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public Timestamp getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Timestamp appointmentDate) {
        this.appointmentDate = appointmentDate;
        if (appointmentDate != null) {
            this.date = appointmentDate.toString();
        }
    }

    public String getDate() {
        if (date != null) {
            return date;
        }
        return appointmentDate != null ? appointmentDate.toString() : "";
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAppointmentTime() {
        return appointmentTime != null ? appointmentTime : "10:00";
    }

    /**
     * Formats appointment time in 12-hour AM/PM format (e.g. '10:00 AM' or '02:30 PM') for UI display.
     * 
     * @return Formatted AM/PM time string
     */
    public String getFormattedTime() {
        if (appointmentTime == null || appointmentTime.trim().isEmpty()) {
            return "10:00 AM";
        }
        try {
            String cleanTime = appointmentTime.trim();
            if (cleanTime.toUpperCase().endsWith("AM") || cleanTime.toUpperCase().endsWith("PM")) {
                return cleanTime.toUpperCase();
            }
            java.time.LocalTime t = java.time.LocalTime.parse(cleanTime);
            return t.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US)).toUpperCase();
        } catch (Exception e) {
            return appointmentTime;
        }
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : "UNPAID";
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
        if (doctor != null) {
            this.doctorId = doctor.getDoctorId();
            this.doctorName = doctor.getDoctorName();
            this.doctorSpecialization = doctor.getSpecialization();
        }
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        if (patient != null) {
            this.patientId = patient.getPatientId();
            this.patientName = patient.getPatientName();
        }
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public int getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(int reminderSent) {
        this.reminderSent = reminderSent;
    }

    public boolean isReminderSent() {
        return reminderSent == 1;
    }

    public void displayAppointment() {
        System.out.println("Appointment ID: " + appointmentId);
        System.out.println("Date: " + getDate());
        System.out.println("Time: " + getAppointmentTime());
        System.out.println("Status: " + getStatus());
        System.out.println("Payment: " + getPaymentStatus());
        if (doctor != null) {
            doctor.displayDoctor();
        }
        if (patient != null) {
            patient.displayPatient();
        }
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId=" + appointmentId +
                ", doctorId=" + doctorId +
                ", patientId=" + patientId +
                ", appointmentDate=" + appointmentDate +
                ", status='" + status + '\'' +
                ", appointmentTime='" + appointmentTime + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}
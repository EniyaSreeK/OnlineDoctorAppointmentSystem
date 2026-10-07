package com.odas;

import java.sql.Timestamp;

/**
 * Prescription entity representing medical diagnoses and treatment regimens.
 * 
 * Purpose (for viva):
 * - Maps to the PRESCRIPTIONS table in Oracle 21c XE.
 * - Links consultation outcomes to APPOINTMENTS, DOCTORS, and PATIENTS.
 * - Stores medical diagnoses and detailed medication instructions (CLOB support).
 */
public class Prescription {

    private int id;
    private int appointmentId;
    private int doctorId;
    private int patientId;
    private String diagnosis;
    private String prescriptionDetails;
    private Timestamp createdDate;

    // Enriched fields for display in dashboards and receipt views
    private String doctorName;
    private String doctorSpecialization;
    private String patientName;
    private Timestamp appointmentDate;

    public Prescription() {
    }

    public Prescription(int appointmentId, int doctorId, int patientId, String diagnosis, String prescriptionDetails) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.diagnosis = diagnosis;
        this.prescriptionDetails = prescriptionDetails;
    }

    public Prescription(int id, int appointmentId, int doctorId, int patientId, 
                        String diagnosis, String prescriptionDetails, Timestamp createdDate) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.diagnosis = diagnosis;
        this.prescriptionDetails = prescriptionDetails;
        this.createdDate = createdDate;
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

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescriptionDetails() {
        return prescriptionDetails;
    }

    public void setPrescriptionDetails(String prescriptionDetails) {
        this.prescriptionDetails = prescriptionDetails;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
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

    public Timestamp getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Timestamp appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    @Override
    public String toString() {
        return "Prescription{" +
                "id=" + id +
                ", appointmentId=" + appointmentId +
                ", doctorId=" + doctorId +
                ", patientId=" + patientId +
                ", diagnosis='" + diagnosis + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}

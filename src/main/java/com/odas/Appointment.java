package com.odas;

public class Appointment {

    private int appointmentId;
    private Doctor doctor;
    private Patient patient;
    private String date;

    public Appointment(int appointmentId, Doctor doctor, Patient patient, String date) {
        this.appointmentId = appointmentId;
        this.doctor = doctor;
        this.patient = patient;
        this.date = date;
    }

    public void displayAppointment() {
        System.out.println("Appointment ID: " + appointmentId);
        System.out.println("Date: " + date);
        doctor.displayDoctor();
        patient.displayPatient();
    }
}
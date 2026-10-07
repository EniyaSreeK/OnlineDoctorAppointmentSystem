package com.odas;

/**
 * Patient entity representing registered patients.
 * 
 * Purpose (for viva):
 * - Encapsulates patient details such as ID, name, age, gender, contact info, and linked user account.
 * - Used by PatientDAO, ProfileServlet, and Appointment operations to manage patient records.
 */
public class Patient {

    private int patientId;
    private String patientName;
    private int age;
    private Integer userId;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private java.sql.Timestamp createdDate;

    public Patient() {
    }

    public Patient(int patientId, String patientName, int age) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.age = age;
    }

    public Patient(int patientId, String patientName, int age, Integer userId) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.age = age;
        this.userId = userId;
    }

    public Patient(String patientName, int age, Integer userId) {
        this.patientName = patientName;
        this.age = age;
        this.userId = userId;
    }

    public Patient(int patientId, String patientName, int age, Integer userId, 
                   String gender, String phone, String email, String address) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.age = age;
        this.userId = userId;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    // Convenience alias for getId()
    public int getId() {
        return patientId;
    }

    public void setId(int id) {
        this.patientId = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    // Convenience alias for getName()
    public String getName() {
        return patientName;
    }

    public void setName(String name) {
        this.patientName = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public java.sql.Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(java.sql.Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getFormattedCreatedDate() {
        if (createdDate == null) return "-";
        return createdDate.toLocalDateTime().toLocalDate().toString();
    }

    public void displayPatient() {
        System.out.println("Patient ID: " + patientId);
        System.out.println("Patient Name: " + patientName);
        System.out.println("Age: " + age);
        if (gender != null) System.out.println("Gender: " + gender);
        if (phone != null) System.out.println("Phone: " + phone);
        if (email != null) System.out.println("Email: " + email);
        if (address != null) System.out.println("Address: " + address);
    }

    @Override
    public String toString() {
        return "Patient{" +
                "patientId=" + patientId +
                ", patientName='" + patientName + '\'' +
                ", age=" + age +
                ", userId=" + userId +
                ", gender='" + gender + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
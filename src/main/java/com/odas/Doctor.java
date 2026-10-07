package com.odas;

/**
 * Doctor entity representing medical practitioners.
 * 
 * Purpose (for viva):
 * - Encapsulates doctor details such as ID, name, specialization, qualifications, experience, location, schedule, and linked user account.
 * - Used by DoctorDAO, DoctorServlet, and Admin operations to pass data across application layers.
 */
public class Doctor {

    private int doctorId;
    private String doctorName;
    private String specialization;
    private Integer userId;
    private String qualification;
    private Integer experience;
    private String phone;
    private String email;
    private String location;
    private String availableDays;
    private String availableTime;
    private Double consultationFee = 500.0;
    private java.sql.Timestamp createdDate;

    public Doctor() {
    }

    public Doctor(int doctorId, String doctorName, String specialization) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
    }

    public Doctor(int doctorId, String doctorName, String specialization, Integer userId) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.userId = userId;
    }

    public Doctor(String doctorName, String specialization, Integer userId) {
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.userId = userId;
    }

    public Doctor(int doctorId, String doctorName, String specialization, Integer userId,
                  String qualification, Integer experience, String phone, String email,
                  String location, String availableDays, String availableTime) {
        this(doctorId, doctorName, specialization, userId, qualification, experience, phone, email, location, availableDays, availableTime, 500.0);
    }

    public Doctor(int doctorId, String doctorName, String specialization, Integer userId,
                  String qualification, Integer experience, String phone, String email,
                  String location, String availableDays, String availableTime, Double consultationFee) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.userId = userId;
        this.qualification = qualification;
        this.experience = experience;
        this.phone = phone;
        this.email = email;
        this.location = location;
        this.availableDays = availableDays;
        this.availableTime = availableTime;
        this.consultationFee = (consultationFee != null) ? consultationFee : 500.0;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    // Convenience alias for getId()
    public int getId() {
        return doctorId;
    }

    public void setId(int id) {
        this.doctorId = id;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    // Convenience alias for getName()
    public String getName() {
        return doctorName;
    }

    public void setName(String name) {
        this.doctorName = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public String getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(String availableTime) {
        this.availableTime = availableTime;
    }

    public Double getConsultationFee() {
        return consultationFee != null ? consultationFee : 500.0;
    }

    public void setConsultationFee(Double consultationFee) {
        this.consultationFee = consultationFee;
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

    public void displayDoctor() {
        System.out.println("Doctor ID: " + doctorId);
        System.out.println("Doctor Name: " + doctorName);
        System.out.println("Specialization: " + specialization);
        if (qualification != null) System.out.println("Qualification: " + qualification);
        if (experience != null) System.out.println("Experience: " + experience + " years");
        if (location != null) System.out.println("Location: " + location);
        if (availableDays != null) System.out.println("Available Days: " + availableDays);
        if (availableTime != null) System.out.println("Available Time: " + availableTime);
        System.out.println("Consultation Fee: ₹" + getConsultationFee());
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "doctorId=" + doctorId +
                ", doctorName='" + doctorName + '\'' +
                ", specialization='" + specialization + '\'' +
                ", userId=" + userId +
                ", qualification='" + qualification + '\'' +
                ", experience=" + experience +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", location='" + location + '\'' +
                ", availableDays='" + availableDays + '\'' +
                ", availableTime='" + availableTime + '\'' +
                ", consultationFee=" + consultationFee +
                '}';
    }
}
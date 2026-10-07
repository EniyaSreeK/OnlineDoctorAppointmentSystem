package com.odas;

import com.odas.dao.DoctorDAO;
import com.odas.dao.UserDAO;
import com.odas.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AdminDoctorServlet handles administrative CRUD operations on Doctor profiles.
 * 
 * Purpose (for viva):
 * - Enables Administrators to Add, Edit, and Delete doctors.
 * - Handles optional Doctor login account provisioning (USERS table creation + password hashing).
 * - Enforces role checks so only ADMIN users can modify doctor records.
 */
@WebServlet("/admin-doctors")
public class AdminDoctorServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminDoctorServlet.class.getName());
    private DoctorDAO doctorDAO;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.doctorDAO = new DoctorDAO();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String action = request.getParameter("action");
        if (action != null) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed for doctor modifications. Please use POST.");
            return;
        }

        response.sendRedirect("admin-dashboard.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("delete".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    int doctorId = Integer.parseInt(idParam.trim());
                    Doctor doc = doctorDAO.getDoctorById(doctorId);
                    if (doc == null) {
                        response.sendRedirect("admin-dashboard.jsp?error=not_found");
                        return;
                    }
                    boolean deleted = doctorDAO.deleteDoctor(doctorId);
                    if (deleted) {
                        if (doc.getUserId() != null) {
                            userDAO.deleteUser(doc.getUserId());
                        }
                        LOGGER.info("Admin deleted doctor ID: " + doctorId);
                        response.sendRedirect("admin-dashboard.jsp?msg=deleted&reportTab=doctors");
                        return;
                    } else {
                        LOGGER.warning("Doctor ID=" + doctorId + " could not be deleted (has linked records).");
                        response.sendRedirect("admin-dashboard.jsp?error=doctor_has_records&reportTab=doctors");
                        return;
                    }
                }
            } else if ("add".equalsIgnoreCase(action)) {
                String name = request.getParameter("name");
                String specialization = request.getParameter("specialization");
                String qualification = request.getParameter("qualification");
                String expStr = request.getParameter("experience");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String location = request.getParameter("location");
                String availableDays = request.getParameter("availableDays");
                String availableTime = request.getParameter("availableTime");
                String feeStr = request.getParameter("consultationFee");

                String doctorUsername = request.getParameter("username");
                String doctorPassword = request.getParameter("password");

                String validationError = com.odas.util.ValidationUtil.validateDoctorProfile(
                        name, specialization, qualification, expStr, phone, email, location, availableDays, availableTime, feeStr);

                if (validationError != null) {
                    response.sendRedirect("admin-dashboard.jsp?error=" + java.net.URLEncoder.encode(validationError, java.nio.charset.StandardCharsets.UTF_8));
                    return;
                }

                name = name.trim();
                specialization = specialization.trim();
                Integer userId = null;

                // Provision login account if username and password supplied
                if (doctorUsername != null && !doctorUsername.trim().isEmpty() &&
                    doctorPassword != null && !doctorPassword.isEmpty()) {

                    doctorUsername = doctorUsername.trim();

                    if (userDAO.usernameExists(doctorUsername)) {
                        response.sendRedirect("admin-dashboard.jsp?error=username_exists&reportTab=doctors");
                        return;
                    }

                    String hashedPassword = PasswordUtil.hashPassword(doctorPassword);
                    User doctorUser = new User(doctorUsername, hashedPassword, "DOCTOR");
                    int newUserId = userDAO.createUser(doctorUser);
                    if (newUserId > 0) {
                        userId = newUserId;
                    }
                }

                Doctor newDoctor = new Doctor(name, specialization, userId);
                if (qualification != null && !qualification.trim().isEmpty()) newDoctor.setQualification(qualification.trim());
                if (expStr != null && !expStr.trim().isEmpty()) newDoctor.setExperience(Integer.parseInt(expStr.trim()));
                if (phone != null && !phone.trim().isEmpty()) newDoctor.setPhone(phone.trim());
                if (email != null && !email.trim().isEmpty()) newDoctor.setEmail(email.trim());
                if (location != null && !location.trim().isEmpty()) newDoctor.setLocation(location.trim());
                if (availableDays != null && !availableDays.trim().isEmpty()) newDoctor.setAvailableDays(availableDays.trim());
                if (availableTime != null && !availableTime.trim().isEmpty()) newDoctor.setAvailableTime(availableTime.trim());
                if (feeStr != null && !feeStr.trim().isEmpty()) {
                    newDoctor.setConsultationFee(Double.parseDouble(feeStr.trim()));
                } else {
                    newDoctor.setConsultationFee(500.0);
                }

                int doctorId = doctorDAO.addDoctor(newDoctor);

                if (doctorId > 0) {
                    LOGGER.info("Admin successfully created new doctor ID=" + doctorId + " (" + name + ")");
                    response.sendRedirect("admin-dashboard.jsp?msg=added&reportTab=doctors");
                } else {
                    if (userId != null) {
                        userDAO.deleteUser(userId);
                    }
                    response.sendRedirect("admin-dashboard.jsp?error=add_failed&reportTab=doctors");
                }

            } else if ("edit".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                String name = request.getParameter("name");
                String specialization = request.getParameter("specialization");
                String qualification = request.getParameter("qualification");
                String expStr = request.getParameter("experience");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String location = request.getParameter("location");
                String availableDays = request.getParameter("availableDays");
                String availableTime = request.getParameter("availableTime");
                String feeStr = request.getParameter("consultationFee");

                if (idParam == null || idParam.trim().isEmpty()) {
                    response.sendRedirect("admin-dashboard.jsp?error=missing_fields&reportTab=doctors");
                    return;
                }

                int doctorId = Integer.parseInt(idParam.trim());

                String validationError = com.odas.util.ValidationUtil.validateDoctorProfile(
                        name, specialization, qualification, expStr, phone, email, location, availableDays, availableTime, feeStr);

                if (validationError != null) {
                    response.sendRedirect("admin-dashboard.jsp?editId=" + doctorId + "&reportTab=doctors&error=" + java.net.URLEncoder.encode(validationError, java.nio.charset.StandardCharsets.UTF_8));
                    return;
                }

                Doctor existing = doctorDAO.getDoctorById(doctorId);
                if (existing != null) {
                    existing.setName(name.trim());
                    existing.setSpecialization(specialization.trim());
                    existing.setQualification(qualification != null ? qualification.trim() : null);
                    if (expStr != null && !expStr.trim().isEmpty()) {
                        existing.setExperience(Integer.parseInt(expStr.trim()));
                    } else {
                        existing.setExperience(null);
                    }
                    existing.setPhone(phone != null ? phone.trim() : null);
                    existing.setEmail(email != null ? email.trim() : null);
                    existing.setLocation(location != null ? location.trim() : null);
                    existing.setAvailableDays(availableDays != null ? availableDays.trim() : null);
                    existing.setAvailableTime(availableTime != null ? availableTime.trim() : null);
                    if (feeStr != null && !feeStr.trim().isEmpty()) {
                        existing.setConsultationFee(Double.parseDouble(feeStr.trim()));
                    } else {
                        existing.setConsultationFee(500.0);
                    }

                    boolean updated = doctorDAO.updateDoctor(existing);
                    if (updated) {
                        LOGGER.info("Admin successfully updated doctor ID=" + doctorId);
                        response.sendRedirect("admin-dashboard.jsp?msg=updated&reportTab=doctors");
                        return;
                    }
                }
                response.sendRedirect("admin-dashboard.jsp?error=update_failed&reportTab=doctors");

            } else {
                response.sendRedirect("admin-dashboard.jsp");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in AdminDoctorServlet", e);
            response.sendRedirect("admin-dashboard.jsp?error=server_error");
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && "ADMIN".equals(session.getAttribute("role"));
    }
}

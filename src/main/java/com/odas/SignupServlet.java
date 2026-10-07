package com.odas;

import com.odas.dao.PatientDAO;
import com.odas.dao.UserDAO;
import com.odas.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SignupServlet handles patient registration.
 * 
 * Purpose (for viva):
 * - Validates registration parameters (username uniqueness, input formatting, valid age range).
 * - Hashes password using SHA-256 before database insertion.
 * - Atomically persists User entity in USERS table and Patient entity in PATIENTS table.
 */
@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SignupServlet.class.getName());
    private UserDAO userDAO;
    private PatientDAO patientDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.patientDAO = new PatientDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/signup.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String name = request.getParameter("name");
        String ageStr = request.getParameter("age");
        String gender = request.getParameter("gender");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String address = request.getParameter("address");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // 1. Validate full demographic patient profile via centralized ValidationUtil
        String profileError = com.odas.util.ValidationUtil.validatePatientProfile(name, ageStr, gender, phone, email, address);
        if (profileError != null) {
            sendError(request, response, profileError, name, ageStr, gender, phone, email, address, username);
            return;
        }

        // 2. Validate credentials
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            sendError(request, response, "Username and password are required.", name, ageStr, gender, phone, email, address, username);
            return;
        }

        name = name.trim();
        username = username.trim();
        int age = Integer.parseInt(ageStr.trim());

        // Validate username format and length
        if (username.length() < 3 || username.length() > 50) {
            sendError(request, response, "Username must be between 3 and 50 characters.", name, ageStr, gender, phone, email, address, username);
            return;
        }

        // Validate password length
        if (password.length() < com.odas.util.ValidationUtil.MIN_PASSWORD_LENGTH) {
            sendError(request, response, "Password must be at least " + com.odas.util.ValidationUtil.MIN_PASSWORD_LENGTH + " characters.", name, ageStr, gender, phone, email, address, username);
            return;
        }

        try {
            // Check for duplicate username
            if (userDAO.usernameExists(username)) {
                sendError(request, response, "Username '" + username + "' is already taken. Please choose another.", name, ageStr, gender, phone, email, address, null);
                return;
            }

            // Hash password securely with PBKDF2
            String hashedPassword = PasswordUtil.hashPassword(password);

            // Create and persist user with PATIENT role
            User user = new User(username, hashedPassword, "PATIENT");
            int userId = userDAO.createUser(user);

            if (userId <= 0) {
                sendError(request, response, "Failed to create user account. Please try again.", name, ageStr, gender, phone, email, address, username);
                return;
            }

            // Create and persist full patient record
            Patient patient = new Patient(0, name, age, userId, gender, phone, email, address);
            int patientId = patientDAO.addPatient(patient);

            if (patientId <= 0) {
                // Cleanup user record if patient creation failed
                userDAO.deleteUser(userId);
                sendError(request, response, "Failed to create patient profile. Please try again.", name, ageStr, gender, phone, email, address, username);
                return;
            }

            LOGGER.info("Successfully registered new patient: " + username + " (Patient ID: " + patientId + ")");
            response.sendRedirect(request.getContextPath() + "/login.jsp?registered=true");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during patient signup", e);
            sendError(request, response, "An unexpected error occurred. Please try again later.", name, ageStr, gender, phone, email, address, username);
        }
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response,
                           String message, String name, String age, String gender,
                           String phone, String email, String address, String username)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        request.setAttribute("name", name);
        request.setAttribute("age", age);
        request.setAttribute("gender", gender);
        request.setAttribute("phone", phone);
        request.setAttribute("email", email);
        request.setAttribute("address", address);
        request.setAttribute("username", username);
        request.getRequestDispatcher("/signup.jsp").forward(request, response);
    }
}

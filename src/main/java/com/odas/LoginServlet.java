package com.odas;

import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
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
 * LoginServlet manages user authentication, password verification, and session creation.
 * 
 * Purpose (for viva):
 * - Authenticates users against the Oracle USERS table using UserDAO.
 * - Compares cryptographic SHA-256 hashes using PasswordUtil.
 * - Prevents session fixation by invalidating prior sessions and creating a fresh HttpSession.
 * - Implements role-based redirection to PATIENT, DOCTOR, or ADMIN dashboards.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private UserDAO userDAO;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.patientDAO = new PatientDAO();
        this.doctorDAO = new DoctorDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // If user already logged in, redirect to their role dashboard
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("role") != null) {
            String role = (String) session.getAttribute("role");
            redirectToDashboard(response, request.getContextPath(), role);
            return;
        }

        // Otherwise forward to login page
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validate required inputs
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("errorMessage", "Username and password are required.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        username = username.trim();

        try {
            User user = userDAO.findByUsername(username);

            boolean passwordMatches = false;
            if (user != null) {
                passwordMatches = PasswordUtil.checkPassword(password, user.getPassword());
            }

            if (user != null && passwordMatches) {
                // Invalidate old session to mitigate session fixation attacks
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                // Create new authenticated session
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("role", user.getRole());
                session.setAttribute("csrfToken", com.odas.util.CsrfFilter.generateToken());

                // Auto-upgrade legacy password hash to PBKDF2
                if (PasswordUtil.needsUpgrade(user.getPassword())) {
                    String upgradedHash = PasswordUtil.hashPassword(password);
                    userDAO.updatePassword(user.getId(), upgradedHash);
                    LOGGER.info("Upgraded password hash to PBKDF2 for user: " + username);
                }

                // Cache patient or doctor specific details for quick display
                if ("PATIENT".equals(user.getRole())) {
                    Patient patient = patientDAO.getPatientByUserId(user.getId());
                    if (patient != null) {
                        session.setAttribute("patientId", patient.getId());
                        session.setAttribute("patientName", patient.getName());
                    }
                } else if ("DOCTOR".equals(user.getRole())) {
                    Doctor doctor = doctorDAO.getDoctorByUserId(user.getId());
                    if (doctor != null) {
                        session.setAttribute("doctorId", doctor.getId());
                        session.setAttribute("doctorName", doctor.getName());
                    }
                }

                LOGGER.info("Successful login for user: " + username + " [Role: " + user.getRole() + "]");
                redirectToDashboard(response, request.getContextPath(), user.getRole());
            } else {
                LOGGER.warning("Failed login attempt for user: " + username);
                request.setAttribute("errorMessage", "Invalid username or password.");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
            request.setAttribute("errorMessage", "A system error occurred during authentication. Please try again.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    private void redirectToDashboard(HttpServletResponse response, String contextPath, String role)
            throws IOException {
        if ("PATIENT".equals(role)) {
            response.sendRedirect(contextPath + "/patient-dashboard.jsp");
        } else if ("DOCTOR".equals(role)) {
            response.sendRedirect(contextPath + "/doctor-dashboard.jsp");
        } else if ("ADMIN".equals(role)) {
            response.sendRedirect(contextPath + "/admin-dashboard.jsp");
        } else {
            response.sendRedirect(contextPath + "/login.jsp");
        }
    }
}
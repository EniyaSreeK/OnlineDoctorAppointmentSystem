package com.odas;

import com.odas.dao.PatientDAO;
import com.odas.dao.UserDAO;
import com.odas.util.ValidationUtil;
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
 * AdminPatientServlet handles administrative patient management operations (e.g. deletion, updating).
 * 
 * Purpose (for viva):
 * - Restricts patient modification and deletion to authenticated ADMIN users.
 * - Guards against destroying patient records with active clinical history (ORA-02292 RESTRICT).
 * - Enforces POST-only state changes with CSRF validation via CsrfFilter.
 * - Allows administrative profile updates with demographic and contact validation.
 */
@WebServlet("/admin-patients")
public class AdminPatientServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminPatientServlet.class.getName());

    private PatientDAO patientDAO;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.patientDAO = new PatientDAO();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed for patient management actions. Please use POST.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        if ("edit".equalsIgnoreCase(action) && idParam != null && !idParam.trim().isEmpty()) {
            try {
                int patientId = Integer.parseInt(idParam.trim());
                Patient patient = patientDAO.getPatientById(patientId);
                if (patient == null) {
                    response.sendRedirect("admin-dashboard.jsp?err=patient_not_found&tab=patients");
                    return;
                }

                String name = request.getParameter("name");
                String ageParam = request.getParameter("age");
                String gender = request.getParameter("gender");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String address = request.getParameter("address");

                if (name == null || !ValidationUtil.isValidName(name)) {
                    response.sendRedirect("admin-dashboard.jsp?err=" + java.net.URLEncoder.encode("Invalid patient name.", java.nio.charset.StandardCharsets.UTF_8) + "&tab=patients&editPatientId=" + patientId);
                    return;
                }

                int age = -1;
                try {
                    age = Integer.parseInt(ageParam != null ? ageParam.trim() : "");
                } catch (Exception ignored) {}
                if (age < 0 || age > 130) {
                    response.sendRedirect("admin-dashboard.jsp?err=" + java.net.URLEncoder.encode("Age must be between 0 and 130.", java.nio.charset.StandardCharsets.UTF_8) + "&tab=patients&editPatientId=" + patientId);
                    return;
                }

                if (phone != null && !phone.trim().isEmpty() && !ValidationUtil.isValidPhone(phone)) {
                    response.sendRedirect("admin-dashboard.jsp?err=" + java.net.URLEncoder.encode("Phone must be exactly 10 digits.", java.nio.charset.StandardCharsets.UTF_8) + "&tab=patients&editPatientId=" + patientId);
                    return;
                }

                if (email != null && !email.trim().isEmpty() && !ValidationUtil.isValidEmail(email)) {
                    response.sendRedirect("admin-dashboard.jsp?err=" + java.net.URLEncoder.encode("Invalid email address format.", java.nio.charset.StandardCharsets.UTF_8) + "&tab=patients&editPatientId=" + patientId);
                    return;
                }

                // Check duplicate email
                if (email != null && !email.trim().isEmpty() && patientDAO.isEmailTakenExcept(email.trim(), patientId)) {
                    response.sendRedirect("admin-dashboard.jsp?err=" + java.net.URLEncoder.encode("Email address is already registered to another patient.", java.nio.charset.StandardCharsets.UTF_8) + "&tab=patients&editPatientId=" + patientId);
                    return;
                }

                // Check duplicate phone
                if (phone != null && !phone.trim().isEmpty() && patientDAO.isPhoneTakenExcept(phone.trim(), patientId)) {
                    response.sendRedirect("admin-dashboard.jsp?err=" + java.net.URLEncoder.encode("Phone number is already registered to another patient.", java.nio.charset.StandardCharsets.UTF_8) + "&tab=patients&editPatientId=" + patientId);
                    return;
                }

                patient.setName(name.trim());
                patient.setAge(age);
                patient.setGender(gender != null && !gender.trim().isEmpty() ? gender.trim() : null);
                patient.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
                patient.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
                patient.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);

                boolean updated = patientDAO.updatePatient(patient);
                if (updated) {
                    LOGGER.info("Admin updated patient ID=" + patientId + " successfully.");
                    response.sendRedirect("admin-dashboard.jsp?msg=patient_updated&tab=patients");
                } else {
                    response.sendRedirect("admin-dashboard.jsp?err=patient_update_failed&tab=patients");
                }
                return;
            } catch (NumberFormatException e) {
                response.sendRedirect("admin-dashboard.jsp?err=invalid_id&tab=patients");
                return;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error editing patient", e);
                response.sendRedirect("admin-dashboard.jsp?err=patient_update_failed&tab=patients");
                return;
            }
        }

        if ("delete".equalsIgnoreCase(action) && idParam != null && !idParam.trim().isEmpty()) {
            try {
                int patientId = Integer.parseInt(idParam.trim());
                Patient patient = patientDAO.getPatientById(patientId);

                if (patient == null) {
                    response.sendRedirect("admin-dashboard.jsp?err=patient_not_found&tab=patients");
                    return;
                }

                // Guarded deletion: if patient has clinical records, deletePatient returns false (ORA-02292)
                boolean patientDeleted = patientDAO.deletePatient(patientId);
                if (patientDeleted) {
                    if (patient.getUserId() != null) {
                        userDAO.deleteUser(patient.getUserId());
                    }
                    LOGGER.info("Admin deleted patient ID=" + patientId + " and linked user ID=" + patient.getUserId());
                    response.sendRedirect("admin-dashboard.jsp?msg=patient_deleted&tab=patients");
                } else {
                    LOGGER.warning("Admin patient deletion prevented for ID=" + patientId + ": active appointment records exist.");
                    response.sendRedirect("admin-dashboard.jsp?err=patient_has_records&tab=patients");
                }
            } catch (NumberFormatException e) {
                response.sendRedirect("admin-dashboard.jsp?err=invalid_id&tab=patients");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error deleting patient", e);
                response.sendRedirect("admin-dashboard.jsp?err=patient_delete_failed&tab=patients");
            }
        } else {
            response.sendRedirect("admin-dashboard.jsp?tab=patients");
        }
    }
}

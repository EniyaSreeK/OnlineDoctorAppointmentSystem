package com.odas;

import com.odas.dao.PatientDAO;
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
 * ProfileServlet manages viewing and updating patient demographic details.
 * 
 * Purpose (for viva):
 * - Authenticated patients can view and update their full demographic profile.
 * - Enforces server-side validations: name not empty, age 1-120, gender in fixed list,
 *   phone 10 digits, email format.
 * - Uses PatientDAO with parameterized PreparedStatements.
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());
    private PatientDAO patientDAO;

    @Override
    public void init() throws ServletException {
        this.patientDAO = new PatientDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !"PATIENT".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Patient patient = patientDAO.getPatientByUserId(userId);

        if (patient == null) {
            response.sendRedirect("patient-dashboard.jsp?error=profile_not_found");
            return;
        }

        request.setAttribute("patient", patient);
        request.getRequestDispatcher("/patient-profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !"PATIENT".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Patient existingPatient = patientDAO.getPatientByUserId(userId);
        if (existingPatient == null) {
            response.sendRedirect("patient-dashboard.jsp?error=profile_not_found");
            return;
        }

        String name = request.getParameter("name");
        String ageStr = request.getParameter("age");
        String gender = request.getParameter("gender");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String address = request.getParameter("address");

        // Validate demographic fields
        String validationError = ValidationUtil.validatePatientProfile(name, ageStr, gender, phone, email, address);

        if (validationError != null) {
            // Keep user input in the form
            Patient temp = new Patient();
            temp.setId(existingPatient.getId());
            temp.setUserId(userId);
            temp.setName(name);
            try {
                if (ageStr != null && !ageStr.trim().isEmpty()) {
                    temp.setAge(Integer.parseInt(ageStr.trim()));
                }
            } catch (NumberFormatException ignored) {}
            temp.setGender(gender);
            temp.setPhone(phone);
            temp.setEmail(email);
            temp.setAddress(address);

            request.setAttribute("patient", temp);
            request.setAttribute("errorMessage", validationError);
            request.getRequestDispatcher("/patient-profile.jsp").forward(request, response);
            return;
        }

        // Apply validated changes
        existingPatient.setName(name.trim());
        existingPatient.setAge(Integer.parseInt(ageStr.trim()));
        existingPatient.setGender(gender.trim());
        existingPatient.setPhone(phone.trim());
        existingPatient.setEmail(email.trim());
        existingPatient.setAddress(address != null ? address.trim() : "");

        try {
            boolean updated = patientDAO.updatePatient(existingPatient);
            if (updated) {
                session.setAttribute("patientName", existingPatient.getName());
                LOGGER.info("Patient profile updated successfully for userId=" + userId);
                response.sendRedirect("profile?msg=updated");
            } else {
                request.setAttribute("patient", existingPatient);
                request.setAttribute("errorMessage", "Failed to update profile. Please try again.");
                request.getRequestDispatcher("/patient-profile.jsp").forward(request, response);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating patient profile for userId=" + userId, e);
            request.setAttribute("patient", existingPatient);
            request.setAttribute("errorMessage", "A system error occurred while updating profile.");
            request.getRequestDispatcher("/patient-profile.jsp").forward(request, response);
        }
    }
}

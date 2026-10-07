package com.odas;

import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
import com.odas.dao.PrescriptionDAO;
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
 * PrescriptionServlet coordinates medical prescription issuance and consultation notes.
 * 
 * Purpose (for viva):
 * - Enforces role-based clinical authorization: only the attending DOCTOR (or ADMIN) can author prescriptions for COMPLETED appointments.
 * - Protects patient privacy: patients can only review prescriptions issued directly to their profile.
 * - Persists detailed medication instructions and diagnoses to Oracle PRESCRIPTIONS table via PrescriptionDAO.
 * - Forwards to prescription-form.jsp (authoring) and prescription-view.jsp (clinical receipt / print view).
 */
@WebServlet("/prescription")
public class PrescriptionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PrescriptionServlet.class.getName());

    private PrescriptionDAO prescriptionDAO;
    private AppointmentDAO appointmentDAO;
    private DoctorDAO doctorDAO;
    private PatientDAO patientDAO;

    @Override
    public void init() throws ServletException {
        this.prescriptionDAO = new PrescriptionDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.doctorDAO = new DoctorDAO();
        this.patientDAO = new PatientDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");
        String action = request.getParameter("action");
        String apptIdParam = request.getParameter("appointmentId");
        String presIdParam = request.getParameter("id");

        // ---------------------------------------------------------------------
        // Action: Form to Add/Create Prescription
        // ---------------------------------------------------------------------
        if ("add".equalsIgnoreCase(action) || "form".equalsIgnoreCase(action)) {
            if (!"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
                response.sendRedirect("patient-dashboard.jsp?error=unauthorized");
                return;
            }

            if (apptIdParam == null || apptIdParam.trim().isEmpty()) {
                response.sendRedirect("doctor-dashboard.jsp?error=missing_appointment_id");
                return;
            }

            int appointmentId;
            try {
                appointmentId = Integer.parseInt(apptIdParam.trim());
            } catch (NumberFormatException e) {
                response.sendRedirect("doctor-dashboard.jsp?error=invalid_appointment_id");
                return;
            }

            Appointment appt = appointmentDAO.getAppointmentById(appointmentId);
            if (appt == null) {
                response.sendRedirect("doctor-dashboard.jsp?error=appointment_not_found");
                return;
            }

            // Only completed appointments can have prescriptions issued
            if (!"COMPLETED".equalsIgnoreCase(appt.getStatus())) {
                response.sendRedirect("doctor-dashboard.jsp?error=only_completed_can_prescribe");
                return;
            }

            // Ownership check for Doctor
            if ("DOCTOR".equals(role)) {
                Integer doctorId = (Integer) session.getAttribute("doctorId");
                if (doctorId == null) {
                    Integer userId = (Integer) session.getAttribute("userId");
                    Doctor d = doctorDAO.getDoctorByUserId(userId);
                    if (d != null) {
                        doctorId = d.getId();
                        session.setAttribute("doctorId", doctorId);
                        session.setAttribute("doctorName", d.getName());
                    }
                }
                if (doctorId == null || appt.getDoctorId() != doctorId) {
                    response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                    return;
                }
            }

            // If prescription already exists, redirect to view
            Prescription existing = prescriptionDAO.getPrescriptionByAppointmentId(appointmentId);
            if (existing != null) {
                response.sendRedirect("prescription?id=" + existing.getId() + "&msg=already_exists");
                return;
            }

            request.setAttribute("appointment", appt);
            request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
            return;
        }

        // ---------------------------------------------------------------------
        // Action: Form to Edit Existing Prescription (SRS FR10 & Section 8)
        // ---------------------------------------------------------------------
        if ("edit".equalsIgnoreCase(action)) {
            if (!"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
                response.sendRedirect("patient-dashboard.jsp?error=unauthorized");
                return;
            }

            Prescription existing = null;
            if (presIdParam != null && !presIdParam.trim().isEmpty()) {
                try {
                    existing = prescriptionDAO.getPrescriptionById(Integer.parseInt(presIdParam.trim()));
                } catch (NumberFormatException ignored) {}
            } else if (apptIdParam != null && !apptIdParam.trim().isEmpty()) {
                try {
                    existing = prescriptionDAO.getPrescriptionByAppointmentId(Integer.parseInt(apptIdParam.trim()));
                } catch (NumberFormatException ignored) {}
            }

            if (existing == null) {
                response.sendRedirect("doctor-dashboard.jsp?error=prescription_not_found");
                return;
            }

            // Ownership check for Doctor
            if ("DOCTOR".equals(role)) {
                Integer doctorId = (Integer) session.getAttribute("doctorId");
                if (doctorId == null) {
                    Integer userId = (Integer) session.getAttribute("userId");
                    Doctor d = doctorDAO.getDoctorByUserId(userId);
                    if (d != null) {
                        doctorId = d.getId();
                        session.setAttribute("doctorId", doctorId);
                        session.setAttribute("doctorName", d.getName());
                    }
                }
                if (doctorId == null || existing.getDoctorId() != doctorId) {
                    response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                    return;
                }
            }

            Appointment appt = appointmentDAO.getAppointmentById(existing.getAppointmentId());
            if (appt == null) {
                response.sendRedirect("doctor-dashboard.jsp?error=appointment_not_found");
                return;
            }

            request.setAttribute("isEdit", true);
            request.setAttribute("prescription", existing);
            request.setAttribute("appointment", appt);
            request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
            return;
        }

        // ---------------------------------------------------------------------
        // Action: View / Download / Print Prescription
        // ---------------------------------------------------------------------
        Prescription prescription = null;
        if (presIdParam != null && !presIdParam.trim().isEmpty()) {
            try {
                int presId = Integer.parseInt(presIdParam.trim());
                prescription = prescriptionDAO.getPrescriptionById(presId);
            } catch (NumberFormatException ignored) {}
        } else if (apptIdParam != null && !apptIdParam.trim().isEmpty()) {
            try {
                int apptId = Integer.parseInt(apptIdParam.trim());
                prescription = prescriptionDAO.getPrescriptionByAppointmentId(apptId);
            } catch (NumberFormatException ignored) {}
        }

        if (prescription == null) {
            if ("PATIENT".equals(role)) {
                response.sendRedirect("patient-dashboard.jsp?error=prescription_not_found");
            } else {
                response.sendRedirect("doctor-dashboard.jsp?error=prescription_not_found");
            }
            return;
        }

        // Authorization check for viewing
        if ("PATIENT".equals(role)) {
            Integer patientId = (Integer) session.getAttribute("patientId");
            if (patientId == null) {
                Integer userId = (Integer) session.getAttribute("userId");
                Patient p = patientDAO.getPatientByUserId(userId);
                if (p != null) {
                    patientId = p.getId();
                    session.setAttribute("patientId", patientId);
                    session.setAttribute("patientName", p.getName());
                }
            }
            if (patientId == null || prescription.getPatientId() != patientId) {
                response.sendRedirect("patient-dashboard.jsp?error=unauthorized");
                return;
            }
        } else if ("DOCTOR".equals(role)) {
            Integer doctorId = (Integer) session.getAttribute("doctorId");
            if (doctorId == null) {
                Integer userId = (Integer) session.getAttribute("userId");
                Doctor d = doctorDAO.getDoctorByUserId(userId);
                if (d != null) {
                    doctorId = d.getId();
                    session.setAttribute("doctorId", doctorId);
                    session.setAttribute("doctorName", d.getName());
                }
            }
            if (doctorId == null || prescription.getDoctorId() != doctorId) {
                response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                return;
            }
        }

        // Load related appointment and doctor details if needed
        Appointment appt = appointmentDAO.getAppointmentById(prescription.getAppointmentId());
        request.setAttribute("prescription", prescription);
        request.setAttribute("appointment", appt);
        request.getRequestDispatcher("/prescription-view.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");
        if (!"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
            response.sendRedirect("patient-dashboard.jsp?error=unauthorized");
            return;
        }

        String action = request.getParameter("action");
        String apptIdParam = request.getParameter("appointmentId");
        String diagnosis = request.getParameter("diagnosis");
        String prescriptionDetails = request.getParameter("prescriptionDetails");

        // ---------------------------------------------------------------------
        // Action: Update Existing Prescription (SRS FR10 & Section 8)
        // ---------------------------------------------------------------------
        if ("edit".equalsIgnoreCase(action)) {
            String presIdParam = request.getParameter("prescriptionId");
            if (presIdParam == null || presIdParam.trim().isEmpty()) {
                presIdParam = request.getParameter("id");
            }
            if (presIdParam == null || presIdParam.trim().isEmpty()) {
                response.sendRedirect("doctor-dashboard.jsp?error=missing_prescription_id");
                return;
            }

            int prescriptionId;
            try {
                prescriptionId = Integer.parseInt(presIdParam.trim());
            } catch (NumberFormatException e) {
                response.sendRedirect("doctor-dashboard.jsp?error=invalid_prescription_id");
                return;
            }

            Prescription existing = prescriptionDAO.getPrescriptionById(prescriptionId);
            if (existing == null) {
                response.sendRedirect("doctor-dashboard.jsp?error=prescription_not_found");
                return;
            }

            // Ownership check for Doctor
            if ("DOCTOR".equals(role)) {
                Integer doctorId = (Integer) session.getAttribute("doctorId");
                if (doctorId == null) {
                    Integer userId = (Integer) session.getAttribute("userId");
                    Doctor d = doctorDAO.getDoctorByUserId(userId);
                    if (d != null) {
                        doctorId = d.getId();
                        session.setAttribute("doctorId", doctorId);
                        session.setAttribute("doctorName", d.getName());
                    }
                }
                if (doctorId == null || existing.getDoctorId() != doctorId) {
                    response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                    return;
                }
            }

            Appointment appt = appointmentDAO.getAppointmentById(existing.getAppointmentId());

            // Input validation
            if (diagnosis == null || diagnosis.trim().isEmpty()) {
                request.setAttribute("errorMessage", "Diagnosis is required.");
                request.setAttribute("isEdit", true);
                request.setAttribute("prescription", existing);
                request.setAttribute("appointment", appt);
                request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
                return;
            }

            if (prescriptionDetails == null || prescriptionDetails.trim().isEmpty()) {
                request.setAttribute("errorMessage", "Prescription details and medication instructions are required.");
                request.setAttribute("isEdit", true);
                request.setAttribute("prescription", existing);
                request.setAttribute("appointment", appt);
                request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
                return;
            }

            existing.setDiagnosis(diagnosis.trim());
            existing.setPrescriptionDetails(prescriptionDetails.trim());
            boolean updated = prescriptionDAO.updatePrescription(existing);
            if (updated) {
                LOGGER.info("Prescription ID=" + existing.getId() + " updated successfully by doctor/admin.");
                response.sendRedirect("prescription?id=" + existing.getId() + "&msg=updated");
            } else {
                request.setAttribute("errorMessage", "Failed to update prescription. Please try again.");
                request.setAttribute("isEdit", true);
                request.setAttribute("prescription", existing);
                request.setAttribute("appointment", appt);
                request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
            }
            return;
        }

        if (apptIdParam == null || apptIdParam.trim().isEmpty()) {
            response.sendRedirect("doctor-dashboard.jsp?error=missing_appointment_id");
            return;
        }

        int appointmentId;
        try {
            appointmentId = Integer.parseInt(apptIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect("doctor-dashboard.jsp?error=invalid_appointment_id");
            return;
        }

        Appointment appt = appointmentDAO.getAppointmentById(appointmentId);
        if (appt == null) {
            response.sendRedirect("doctor-dashboard.jsp?error=appointment_not_found");
            return;
        }

        if (!"COMPLETED".equalsIgnoreCase(appt.getStatus())) {
            response.sendRedirect("doctor-dashboard.jsp?error=only_completed_can_prescribe");
            return;
        }

        // Ownership check for Doctor
        if ("DOCTOR".equals(role)) {
            Integer doctorId = (Integer) session.getAttribute("doctorId");
            if (doctorId == null) {
                Integer userId = (Integer) session.getAttribute("userId");
                Doctor d = doctorDAO.getDoctorByUserId(userId);
                if (d != null) {
                    doctorId = d.getId();
                    session.setAttribute("doctorId", doctorId);
                    session.setAttribute("doctorName", d.getName());
                }
            }
            if (doctorId == null || appt.getDoctorId() != doctorId) {
                response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                return;
            }
        }

        // Prevent duplicate prescriptions: if one already exists, redirect to view
        Prescription existing = prescriptionDAO.getPrescriptionByAppointmentId(appointmentId);
        if (existing != null) {
            response.sendRedirect("prescription?id=" + existing.getId() + "&msg=already_exists");
            return;
        }

        // Input validation
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Diagnosis is required.");
            request.setAttribute("appointment", appt);
            request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
            return;
        }

        if (prescriptionDetails == null || prescriptionDetails.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Prescription details and medication instructions are required.");
            request.setAttribute("appointment", appt);
            request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
            return;
        }

        try {
            Prescription prescription = new Prescription(
                    appt.getId(),
                    appt.getDoctorId(),
                    appt.getPatientId(),
                    diagnosis.trim(),
                    prescriptionDetails.trim()
            );

            int presId = prescriptionDAO.addPrescription(prescription);
            if (presId > 0) {
                LOGGER.info("Prescription ID=" + presId + " created for appointment #" + appt.getId());
                response.sendRedirect("prescription?id=" + presId + "&msg=created");
            } else {
                Prescription duplicate = prescriptionDAO.getPrescriptionByAppointmentId(appt.getId());
                if (duplicate != null) {
                    response.sendRedirect("prescription?id=" + duplicate.getId() + "&msg=already_exists");
                    return;
                }
                request.setAttribute("errorMessage", "Failed to save prescription. Please review input and try again.");
                request.setAttribute("appointment", appt);
                request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating prescription for appointment #" + appt.getId(), e);
            request.setAttribute("errorMessage", "System error saving prescription. Please try again.");
            request.setAttribute("appointment", appt);
            request.getRequestDispatcher("/prescription-form.jsp").forward(request, response);
        }
    }
}

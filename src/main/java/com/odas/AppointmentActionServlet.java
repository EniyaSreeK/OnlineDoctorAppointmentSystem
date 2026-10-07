package com.odas;

import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
import com.odas.notify.NotificationService;
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
 * AppointmentActionServlet manages status lifecycle transitions for appointments.
 * 
 * Purpose (for viva):
 * - Supports appointment cancellation by owning PATIENT or ADMIN (DOCTORs cannot cancel).
 * - Supports marking appointment as completed by owning DOCTOR or ADMIN (PATIENTs cannot mark complete).
 * - Enforces role and ownership checks to prevent unauthorized parameter tampering.
 */
@WebServlet("/appointment-action")
public class AppointmentActionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AppointmentActionServlet.class.getName());
    private AppointmentDAO appointmentDAO;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;

    @Override
    public void init() throws ServletException {
        this.appointmentDAO = new AppointmentDAO();
        this.patientDAO = new PatientDAO();
        this.doctorDAO = new DoctorDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed for appointment actions. Please use POST.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processAction(request, response);
    }

    private void processAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");
        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        if (action == null || idParam == null) {
            response.sendRedirect("index.jsp");
            return;
        }

        int appointmentId;
        try {
            appointmentId = Integer.parseInt(idParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect("index.jsp");
            return;
        }

        try {
            Appointment appt = appointmentDAO.getAppointmentById(appointmentId);
            if (appt == null) {
                redirectWithError(response, role, "Appointment not found.");
                return;
            }

            if ("cancel".equalsIgnoreCase(action)) {
                // Cancel allowed only when status = SCHEDULED (not COMPLETED or already CANCELLED)
                if (!"SCHEDULED".equalsIgnoreCase(appt.getStatus())) {
                    redirectWithError(response, role, "Only active scheduled appointments can be cancelled.");
                    return;
                }

                // Owning PATIENT, attending DOCTOR, or ADMIN can cancel.
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
                    if (patientId == null || appt.getPatientId() != patientId) {
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
                    if (doctorId == null || appt.getDoctorId() != doctorId) {
                        response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                        return;
                    }
                } else if ("ADMIN".equals(role)) {
                    // Admin is authorized to cancel
                } else {
                    redirectWithError(response, role, "unauthorized");
                    return;
                }

                boolean updated = appointmentDAO.cancelAppointment(appointmentId);
                if (updated) {
                    LOGGER.info("Appointment ID=" + appointmentId + " cancelled successfully by " + role + ".");
                    appt.setStatus("CANCELLED");
                    NotificationService.getInstance().sendCancellationConfirmationAsync(appt);

                    if ("ADMIN".equals(role)) {
                        response.sendRedirect("admin-dashboard.jsp?msg=cancelled");
                    } else if ("DOCTOR".equals(role)) {
                        response.sendRedirect("doctor-dashboard.jsp?msg=declined");
                    } else {
                        response.sendRedirect("patient-dashboard.jsp?msg=cancelled");
                    }
                } else {
                    redirectWithError(response, role, "Unable to cancel appointment.");
                }

            } else if ("complete".equalsIgnoreCase(action)) {
                // Complete allowed only when status = SCHEDULED
                if (!"SCHEDULED".equalsIgnoreCase(appt.getStatus())) {
                    redirectWithError(response, role, "Only active scheduled appointments can be marked as completed.");
                    return;
                }

                // Only owning DOCTOR or ADMIN can mark as completed. PATIENTs cannot mark complete.
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
                } else if ("ADMIN".equals(role)) {
                    // Admin is authorized to complete
                } else {
                    // PATIENT or other role is not permitted to mark complete
                    redirectWithError(response, role, "unauthorized");
                    return;
                }

                boolean updated = appointmentDAO.updateStatus(appointmentId, "COMPLETED");
                if (updated) {
                    LOGGER.info("Appointment ID=" + appointmentId + " marked as COMPLETED.");
                    if ("ADMIN".equals(role)) {
                        response.sendRedirect("admin-dashboard.jsp?msg=completed");
                    } else {
                        response.sendRedirect("doctor-dashboard.jsp?msg=completed");
                    }
                } else {
                    redirectWithError(response, role, "Unable to update appointment status.");
                }
            } else {
                response.sendRedirect("index.jsp");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling appointment action: " + action, e);
            redirectWithError(response, role, "An unexpected error occurred.");
        }
    }

    private void redirectWithError(HttpServletResponse response, String role, String error) throws IOException {
        String encoded = java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8);
        if ("DOCTOR".equals(role)) {
            response.sendRedirect("doctor-dashboard.jsp?error=" + encoded);
        } else if ("ADMIN".equals(role)) {
            response.sendRedirect("admin-dashboard.jsp?error=" + encoded);
        } else {
            response.sendRedirect("patient-dashboard.jsp?error=" + encoded);
        }
    }
}

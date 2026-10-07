package com.odas;

import com.odas.dao.AppointmentDAO;
import com.odas.dao.PatientDAO;
import com.odas.dao.PrescriptionDAO;
import com.odas.util.TimeSlotUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PatientDashboardServlet acts as the controller for the Patient Portal.
 * 
 * Purpose (for viva):
 * - Decouples database querying from patient-dashboard.jsp.
 * - Restricts access to authenticated PATIENT users.
 * - Categorizes appointments into upcoming, past history, and proactive 24-hr reminder banners.
 * - Loads prescriptions and forwards models to patient-dashboard.jsp for pure rendering.
 */
@WebServlet(urlPatterns = {"/patient-dashboard", "/patient/dashboard"})
public class PatientDashboardServlet extends HttpServlet {

    private PatientDAO patientDAO;
    private AppointmentDAO appointmentDAO;
    private PrescriptionDAO prescriptionDAO;

    @Override
    public void init() throws ServletException {
        this.patientDAO = new PatientDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.prescriptionDAO = new PrescriptionDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");
        if (!"PATIENT".equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=unauthorized");
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        Integer patientId = (Integer) session.getAttribute("patientId");
        String patientName = (String) session.getAttribute("patientName");

        if (patientId == null) {
            Patient p = patientDAO.getPatientByUserId(userId);
            if (p != null) {
                patientId = p.getId();
                patientName = p.getName();
                session.setAttribute("patientId", patientId);
                session.setAttribute("patientName", patientName);
            }
        }

        if (patientName == null) {
            patientName = (String) session.getAttribute("username");
        }

        List<Appointment> allAppts = patientId != null ? appointmentDAO.getAppointmentsByPatientId(patientId) : new ArrayList<>();
        List<Appointment> upcomingAppts = new ArrayList<>();
        List<Appointment> pastAppts = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for (Appointment a : allAppts) {
            LocalDate apptDate = a.getAppointmentDate().toLocalDateTime().toLocalDate();
            if ("SCHEDULED".equalsIgnoreCase(a.getStatus()) && !apptDate.isBefore(today)) {
                upcomingAppts.add(a);
            } else {
                pastAppts.add(a);
            }
        }

        // Identify scheduled appointments occurring within the next 24 hours
        List<Appointment> reminderAppts = new ArrayList<>();
        for (Appointment a : upcomingAppts) {
            if (TimeSlotUtil.isWithinNext24Hours(a.getAppointmentDate(), a.getAppointmentTime())) {
                reminderAppts.add(a);
            }
        }

        // Load prescriptions issued to this patient
        Map<Integer, Prescription> prescriptionMap = new HashMap<>();
        if (patientId != null) {
            List<Prescription> patientPrescriptions = prescriptionDAO.getPrescriptionsByPatientId(patientId);
            for (Prescription p : patientPrescriptions) {
                prescriptionMap.put(p.getAppointmentId(), p);
            }
        }

        request.setAttribute("patientName", patientName);
        request.setAttribute("allAppts", allAppts);
        request.setAttribute("upcomingAppts", upcomingAppts);
        request.setAttribute("pastAppts", pastAppts);
        request.setAttribute("reminderAppts", reminderAppts);
        request.setAttribute("prescriptionMap", prescriptionMap);

        request.getRequestDispatcher("/patient-dashboard.jsp").forward(request, response);
    }
}

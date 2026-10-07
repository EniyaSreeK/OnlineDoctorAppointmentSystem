package com.odas;

import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PrescriptionDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DoctorDashboardServlet acts as the controller for the Physician Portal.
 * 
 * Purpose (for viva):
 * - Decouples data retrieval and business logic from doctor-dashboard.jsp.
 * - Restricts access to authenticated DOCTOR users.
 * - Fetches assigned appointments, issued prescriptions, schedule info, and patient history before view rendering.
 */
@WebServlet(urlPatterns = {"/doctor-dashboard", "/doctor/dashboard"})
public class DoctorDashboardServlet extends HttpServlet {

    private DoctorDAO doctorDAO;
    private AppointmentDAO appointmentDAO;
    private PrescriptionDAO prescriptionDAO;

    @Override
    public void init() throws ServletException {
        this.doctorDAO = new DoctorDAO();
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
        if (!"DOCTOR".equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=unauthorized");
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        Integer doctorId = (Integer) session.getAttribute("doctorId");
        String doctorName = (String) session.getAttribute("doctorName");
        String specialization = "";

        if (doctorId == null) {
            Doctor doc = doctorDAO.getDoctorByUserId(userId);
            if (doc != null) {
                doctorId = doc.getId();
                doctorName = doc.getName();
                specialization = doc.getSpecialization();
                session.setAttribute("doctorId", doctorId);
                session.setAttribute("doctorName", doctorName);
            }
        } else {
            Doctor doc = doctorDAO.getDoctorById(doctorId);
            if (doc != null) {
                specialization = doc.getSpecialization();
            }
        }

        if (doctorName == null) {
            doctorName = (String) session.getAttribute("username");
        }

        Doctor currentDoctor = doctorId != null ? doctorDAO.getDoctorById(doctorId) : null;
        List<Appointment> doctorAppts = doctorId != null ? appointmentDAO.getAppointmentsByDoctorId(doctorId) : new ArrayList<>();

        Map<Integer, Prescription> prescriptionMap = new HashMap<>();
        if (doctorId != null) {
            List<Prescription> docPrescriptions = prescriptionDAO.getPrescriptionsByDoctorId(doctorId);
            for (Prescription p : docPrescriptions) {
                prescriptionMap.put(p.getAppointmentId(), p);
            }
        }

        int scheduledCount = 0;
        int completedCount = 0;
        for (Appointment a : doctorAppts) {
            if ("SCHEDULED".equalsIgnoreCase(a.getStatus())) scheduledCount++;
            else if ("COMPLETED".equalsIgnoreCase(a.getStatus())) completedCount++;
        }

        // Patient History View (Filtered to THIS Doctor Only)
        String histIdParam = request.getParameter("patientHistoryId");
        List<Appointment> patientHistoryAppts = new ArrayList<>();
        Map<Integer, Prescription> patientHistoryRx = new HashMap<>();
        String historyPatientName = "";

        if (histIdParam != null && !histIdParam.trim().isEmpty() && doctorId != null) {
            try {
                int histPatientId = Integer.parseInt(histIdParam.trim());
                List<Appointment> allPatAppts = appointmentDAO.getAppointmentsByPatientId(histPatientId);
                for (Appointment a : allPatAppts) {
                    if (a.getDoctorId() == doctorId) {
                        patientHistoryAppts.add(a);
                        historyPatientName = a.getPatientName();
                        Prescription p = prescriptionDAO.getPrescriptionByAppointmentId(a.getId());
                        if (p != null) {
                            patientHistoryRx.put(a.getId(), p);
                        }
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        request.setAttribute("doctorName", doctorName);
        request.setAttribute("specialization", specialization);
        request.setAttribute("currentDoctor", currentDoctor);
        request.setAttribute("doctorAppts", doctorAppts);
        request.setAttribute("prescriptionMap", prescriptionMap);
        request.setAttribute("scheduledCount", scheduledCount);
        request.setAttribute("completedCount", completedCount);
        request.setAttribute("patientHistoryAppts", patientHistoryAppts);
        request.setAttribute("patientHistoryRx", patientHistoryRx);
        request.setAttribute("historyPatientName", historyPatientName);

        request.getRequestDispatcher("/doctor-dashboard.jsp").forward(request, response);
    }
}

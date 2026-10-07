package com.odas;

import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
import com.odas.dao.PaymentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminDashboardServlet acts as the controller for the Administrator portal.
 * 
 * Purpose (for viva):
 * - Separates business querying and analytical aggregation from presentation views.
 * - Restricts access to verified ADMIN users.
 * - Computes consultation metrics, revenue summaries, and report filters before forwarding to admin-dashboard.jsp.
 */
@WebServlet(urlPatterns = {"/admin-dashboard", "/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private DoctorDAO doctorDAO;
    private AppointmentDAO appointmentDAO;
    private PatientDAO patientDAO;
    private PaymentDAO paymentDAO;

    @Override
    public void init() throws ServletException {
        this.doctorDAO = new DoctorDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.patientDAO = new PatientDAO();
        this.paymentDAO = new PaymentDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        String username = (String) session.getAttribute("username");

        List<Doctor> doctors = doctorDAO.getAllDoctors();
        List<Appointment> allAppointments = appointmentDAO.getAllAppointments();
        List<Patient> allPatients = patientDAO.getAllPatients();
        double systemTotalRevenue = paymentDAO.getTotalRevenue();

        // Report Tab Switcher parameter (default: appointments)
        String reportTab = request.getParameter("reportTab");
        if (reportTab == null || reportTab.trim().isEmpty()) {
            reportTab = request.getParameter("tab");
        }
        if (reportTab == null || reportTab.trim().isEmpty()) {
            reportTab = "appointments";
        }

        // Messages and Errors from redirects
        String msg = request.getParameter("msg");
        String err = request.getParameter("err");
        if (err == null || err.trim().isEmpty()) {
            err = request.getParameter("error");
        }

        // Appointment Report filter parameters
        String apptFromStr = request.getParameter("apptFrom");
        String apptToStr = request.getParameter("apptTo");
        String apptStatus = request.getParameter("apptStatus");
        if (apptStatus == null) apptStatus = "ALL";

        LocalDate apptFrom = null;
        LocalDate apptTo = null;
        if (apptFromStr != null && !apptFromStr.trim().isEmpty()) {
            try { apptFrom = LocalDate.parse(apptFromStr.trim()); } catch (Exception ignored) {}
        }
        if (apptToStr != null && !apptToStr.trim().isEmpty()) {
            try { apptTo = LocalDate.parse(apptToStr.trim()); } catch (Exception ignored) {}
        }

        List<Appointment> filteredAppts = appointmentDAO.getFilteredAppointments(apptFrom, apptTo, apptStatus);

        long countScheduled = filteredAppts.stream().filter(a -> "SCHEDULED".equalsIgnoreCase(a.getStatus())).count();
        long countCompleted = filteredAppts.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long countCancelled = filteredAppts.stream().filter(a -> "CANCELLED".equalsIgnoreCase(a.getStatus())).count();

        // Payment Report filter parameters
        String payFromStr = request.getParameter("payFrom");
        String payToStr = request.getParameter("payTo");
        String payMode = request.getParameter("payMode");
        if (payMode == null) payMode = "ALL";

        LocalDate payFrom = null;
        LocalDate payTo = null;
        if (payFromStr != null && !payFromStr.trim().isEmpty()) {
            try { payFrom = LocalDate.parse(payFromStr.trim()); } catch (Exception ignored) {}
        }
        if (payToStr != null && !payToStr.trim().isEmpty()) {
            try { payTo = LocalDate.parse(payToStr.trim()); } catch (Exception ignored) {}
        }

        List<Payment> filteredPayments = paymentDAO.getFilteredPayments(payFrom, payTo, payMode);

        double filteredTotalRevenue = 0.0;
        double cardRevenue = 0.0;
        double upiRevenue = 0.0;
        double netBankingRevenue = 0.0;
        long cardCount = 0;
        long upiCount = 0;
        long netBankingCount = 0;

        for (Payment p : filteredPayments) {
            if ("SUCCESS".equalsIgnoreCase(p.getPaymentStatus())) {
                filteredTotalRevenue += p.getAmount();
                if ("Card".equalsIgnoreCase(p.getPaymentMode())) {
                    cardRevenue += p.getAmount();
                    cardCount++;
                } else if ("UPI".equalsIgnoreCase(p.getPaymentMode())) {
                    upiRevenue += p.getAmount();
                    upiCount++;
                } else if ("Net Banking".equalsIgnoreCase(p.getPaymentMode())) {
                    netBankingRevenue += p.getAmount();
                    netBankingCount++;
                }
            }
        }

        // Patient & Doctor Workload mapping
        Map<Integer, Long> patientApptCounts = allAppointments.stream()
                .collect(Collectors.groupingBy(Appointment::getPatientId, Collectors.counting()));

        Map<Integer, Long> doctorApptCounts = allAppointments.stream()
                .collect(Collectors.groupingBy(Appointment::getDoctorId, Collectors.counting()));

        // Registered Patients Report filter parameters
        String patientFromStr = request.getParameter("patientFrom");
        String patientToStr = request.getParameter("patientTo");
        LocalDate patientFrom = null;
        LocalDate patientTo = null;
        if (patientFromStr != null && !patientFromStr.trim().isEmpty()) {
            try { patientFrom = LocalDate.parse(patientFromStr.trim()); } catch (Exception ignored) {}
        }
        if (patientToStr != null && !patientToStr.trim().isEmpty()) {
            try { patientTo = LocalDate.parse(patientToStr.trim()); } catch (Exception ignored) {}
        }
        List<Patient> filteredPatients = patientDAO.getFilteredPatients(patientFrom, patientTo);

        // Registered Doctors Report filter parameters
        String docFromStr = request.getParameter("docFrom");
        String docToStr = request.getParameter("docTo");
        String docSpec = request.getParameter("docSpec");
        LocalDate docFrom = null;
        LocalDate docTo = null;
        if (docFromStr != null && !docFromStr.trim().isEmpty()) {
            try { docFrom = LocalDate.parse(docFromStr.trim()); } catch (Exception ignored) {}
        }
        if (docToStr != null && !docToStr.trim().isEmpty()) {
            try { docTo = LocalDate.parse(docToStr.trim()); } catch (Exception ignored) {}
        }
        List<Doctor> filteredDoctors = doctorDAO.getFilteredDoctors(docFrom, docTo, docSpec);

        // Check if editing a specific doctor
        String editIdParam = request.getParameter("editId");
        Doctor editDoctor = null;
        if (editIdParam != null) {
            try {
                editDoctor = doctorDAO.getDoctorById(Integer.parseInt(editIdParam.trim()));
            } catch (NumberFormatException ignored) {}
        }

        // Check if editing a specific patient
        String editPatientIdParam = request.getParameter("editPatientId");
        Patient editPatient = null;
        if (editPatientIdParam != null) {
            try {
                editPatient = patientDAO.getPatientById(Integer.parseInt(editPatientIdParam.trim()));
            } catch (NumberFormatException ignored) {}
        }

        request.setAttribute("username", username);
        if (msg != null && !msg.trim().isEmpty()) {
            request.setAttribute("msg", msg);
        }
        if (err != null && !err.trim().isEmpty()) {
            request.setAttribute("err", err);
        }
        request.setAttribute("doctors", doctors);
        request.setAttribute("allAppointments", allAppointments);
        request.setAttribute("allPatients", allPatients);
        request.setAttribute("systemTotalRevenueFormatted", String.format(Locale.US, "%.2f", systemTotalRevenue));
        request.setAttribute("reportTab", reportTab);
        request.setAttribute("apptFromStr", apptFromStr);
        request.setAttribute("apptToStr", apptToStr);
        request.setAttribute("apptStatus", apptStatus);
        request.setAttribute("filteredAppts", filteredAppts);
        request.setAttribute("countScheduled", countScheduled);
        request.setAttribute("countCompleted", countCompleted);
        request.setAttribute("countCancelled", countCancelled);
        request.setAttribute("payFromStr", payFromStr);
        request.setAttribute("payToStr", payToStr);
        request.setAttribute("payMode", payMode);
        request.setAttribute("filteredPayments", filteredPayments);
        request.setAttribute("filteredTotalRevenueFormatted", String.format(Locale.US, "%.2f", filteredTotalRevenue));
        request.setAttribute("cardRevenueFormatted", String.format(Locale.US, "%.2f", cardRevenue));
        request.setAttribute("cardCount", cardCount);
        request.setAttribute("upiRevenueFormatted", String.format(Locale.US, "%.2f", upiRevenue));
        request.setAttribute("upiCount", upiCount);
        request.setAttribute("netBankingRevenueFormatted", String.format(Locale.US, "%.2f", netBankingRevenue));
        request.setAttribute("netBankingCount", netBankingCount);
        request.setAttribute("patientApptCounts", patientApptCounts);
        request.setAttribute("doctorApptCounts", doctorApptCounts);
        request.setAttribute("editDoctor", editDoctor);
        request.setAttribute("editPatient", editPatient);
        request.setAttribute("patientFromStr", patientFromStr);
        request.setAttribute("patientToStr", patientToStr);
        request.setAttribute("filteredPatients", filteredPatients);
        request.setAttribute("docFromStr", docFromStr);
        request.setAttribute("docToStr", docToStr);
        request.setAttribute("docSpec", docSpec);
        request.setAttribute("filteredDoctors", filteredDoctors);

        request.getRequestDispatcher("/admin-dashboard.jsp").forward(request, response);
    }
}

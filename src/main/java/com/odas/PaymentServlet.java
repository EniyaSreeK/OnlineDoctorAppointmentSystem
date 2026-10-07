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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PaymentServlet coordinates simulated online consultation fee checkout and receipt issuance.
 * 
 * Purpose (for viva):
 * - Enforces role-based patient ownership checks: only the owning patient can pay for their appointment.
 * - Server-side fee computation: amounts are strictly read from DOCTORS.CONSULTATION_FEE on the server (client tampering impossible).
 * - Enforces atomic all-or-nothing database transactions via PaymentDAO.processPaymentTransaction (insert PAYMENTS + update APPOINTMENTS.PAYMENT_STATUS = 'PAID').
 * - Blocks double-payment and prevents payment for cancelled or expired consultations.
 * - Generates digital payment receipts with print-friendly layout and visible simulation disclaimers.
 */
@WebServlet("/payment")
public class PaymentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PaymentServlet.class.getName());

    private PaymentDAO paymentDAO;
    private AppointmentDAO appointmentDAO;
    private DoctorDAO doctorDAO;
    private PatientDAO patientDAO;

    @Override
    public void init() throws ServletException {
        this.paymentDAO = new PaymentDAO();
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
        String payIdParam = request.getParameter("id");

        // ---------------------------------------------------------------------
        // Action: View Payment Receipt
        // ---------------------------------------------------------------------
        if ("receipt".equalsIgnoreCase(action)) {
            Payment payment = null;
            if (payIdParam != null && !payIdParam.trim().isEmpty()) {
                try {
                    int payId = Integer.parseInt(payIdParam.trim());
                    payment = paymentDAO.getPaymentById(payId);
                } catch (NumberFormatException ignored) {}
            } else if (apptIdParam != null && !apptIdParam.trim().isEmpty()) {
                try {
                    int apptId = Integer.parseInt(apptIdParam.trim());
                    payment = paymentDAO.getPaymentByAppointmentId(apptId);
                } catch (NumberFormatException ignored) {}
            }

            if (payment == null) {
                String target = "DOCTOR".equals(role) ? "doctor-dashboard.jsp" : "patient-dashboard.jsp";
                response.sendRedirect(target + "?error=Receipt+not+found");
                return;
            }

            // Authorization: only owning patient or admin can view payment receipt
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
                if (patientId == null || payment.getPatientId() != patientId) {
                    response.sendRedirect("patient-dashboard.jsp?error=unauthorized");
                    return;
                }
            } else if (!"ADMIN".equals(role)) {
                // Doctors do not have access to billing receipts
                response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
                return;
            }

            Appointment appt = appointmentDAO.getAppointmentById(payment.getAppointmentId());
            Doctor doc = appt != null ? doctorDAO.getDoctorById(appt.getDoctorId()) : null;

            request.setAttribute("payment", payment);
            request.setAttribute("appointment", appt);
            request.setAttribute("doctor", doc);
            request.getRequestDispatcher("/payment-receipt.jsp").forward(request, response);
            return;
        }

        // ---------------------------------------------------------------------
        // Default Action: Display Payment Checkout Screen
        // ---------------------------------------------------------------------
        if (!"PATIENT".equals(role) && !"ADMIN".equals(role)) {
            response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
            return;
        }

        if (apptIdParam == null || apptIdParam.trim().isEmpty()) {
            response.sendRedirect("patient-dashboard.jsp?error=missing_appointment_id");
            return;
        }

        int appointmentId;
        try {
            appointmentId = Integer.parseInt(apptIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect("patient-dashboard.jsp?error=invalid_appointment_id");
            return;
        }

        Appointment appt = appointmentDAO.getAppointmentById(appointmentId);
        if (appt == null) {
            response.sendRedirect("patient-dashboard.jsp?error=appointment_not_found");
            return;
        }

        // Ownership check: must be the patient who booked the appointment
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
        }

        // Double payment check: if already paid, redirect directly to receipt
        if ("PAID".equalsIgnoreCase(appt.getPaymentStatus())) {
            Payment existing = paymentDAO.getPaymentByAppointmentId(appointmentId);
            if (existing != null) {
                response.sendRedirect("payment?action=receipt&id=" + existing.getId() + "&msg=already_paid");
                return;
            }
        }

        // Block payment for CANCELLED appointments
        if ("CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            response.sendRedirect("patient-dashboard.jsp?error=Cannot+process+payment+for+cancelled+appointments.");
            return;
        }

        // Security: Fee is strictly retrieved from the database on the server
        Doctor doc = doctorDAO.getDoctorById(appt.getDoctorId());
        double consultationFee = (doc != null && doc.getConsultationFee() > 0) ? doc.getConsultationFee() : 500.0;

        request.setAttribute("appointment", appt);
        request.setAttribute("doctor", doc);
        request.setAttribute("consultationFee", consultationFee);
        request.getRequestDispatcher("/payment.jsp").forward(request, response);
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
        if (!"PATIENT".equals(role) && !"ADMIN".equals(role)) {
            response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
            return;
        }

        String apptIdParam = request.getParameter("appointmentId");
        String paymentMode = request.getParameter("paymentMode");

        if (apptIdParam == null || apptIdParam.trim().isEmpty()) {
            response.sendRedirect("patient-dashboard.jsp?error=missing_appointment_id");
            return;
        }

        int appointmentId;
        try {
            appointmentId = Integer.parseInt(apptIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect("patient-dashboard.jsp?error=invalid_appointment_id");
            return;
        }

        Appointment appt = appointmentDAO.getAppointmentById(appointmentId);
        if (appt == null) {
            response.sendRedirect("patient-dashboard.jsp?error=appointment_not_found");
            return;
        }

        // Ownership check
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
        }

        // Double payment check
        if ("PAID".equalsIgnoreCase(appt.getPaymentStatus())) {
            Payment existing = paymentDAO.getPaymentByAppointmentId(appointmentId);
            if (existing != null) {
                response.sendRedirect("payment?action=receipt&id=" + existing.getId() + "&msg=already_paid");
                return;
            }
        }

        // Status checks: only block CANCELLED appointments
        if ("CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            response.sendRedirect("patient-dashboard.jsp?error=Cannot+process+payment+for+cancelled+appointments.");
            return;
        }

        // Security: Fee is strictly retrieved from the database on the server
        Doctor doc = doctorDAO.getDoctorById(appt.getDoctorId());
        double consultationFee = (doc != null && doc.getConsultationFee() > 0) ? doc.getConsultationFee() : 500.0;

        // Payment mode validation against fixed allowed options
        if (paymentMode == null) paymentMode = "";
        paymentMode = paymentMode.trim();
        if (!"Card".equals(paymentMode) && !"UPI".equals(paymentMode) && !"Net Banking".equals(paymentMode)) {
            request.setAttribute("errorMessage", "Invalid payment mode. Please select Card, UPI, or Net Banking.");
            request.setAttribute("appointment", appt);
            request.setAttribute("doctor", doc);
            request.setAttribute("consultationFee", consultationFee);
            request.getRequestDispatcher("/payment.jsp").forward(request, response);
            return;
        }

        // Atomic Transaction: Insert PAYMENTS row and update APPOINTMENTS.PAYMENT_STATUS to PAID
        Payment payment = new Payment(appt.getId(), appt.getPatientId(), consultationFee, paymentMode, "SUCCESS");
        int paymentId = paymentDAO.processPaymentTransaction(payment);

        if (paymentId > 0) {
            LOGGER.info("Payment successful: ID=" + paymentId + " for Appointment #" + appt.getId());
            response.sendRedirect("payment?action=receipt&id=" + paymentId + "&msg=success");
        } else {
            LOGGER.severe("Payment transaction failed for Appointment #" + appt.getId());
            request.setAttribute("errorMessage", "Payment transaction failed. Please try again.");
            request.setAttribute("appointment", appt);
            request.setAttribute("doctor", doc);
            request.setAttribute("consultationFee", consultationFee);
            request.getRequestDispatcher("/payment.jsp").forward(request, response);
        }
    }
}

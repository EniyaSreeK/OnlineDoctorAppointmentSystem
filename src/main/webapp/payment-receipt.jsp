<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="com.odas.Payment" %>
<%@ page import="com.odas.Appointment" %>
<%@ page import="com.odas.Doctor" %>
<%
    // Prevent browser caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String role = (String) session.getAttribute("role");
    if (session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    Payment payment = (Payment) request.getAttribute("payment");
    if (payment == null) {
        response.sendRedirect("patient-dashboard.jsp");
        return;
    }

    Appointment appt = (Appointment) request.getAttribute("appointment");
    Doctor doc = (Doctor) request.getAttribute("doctor");

    String returnDashboard = "patient-dashboard.jsp";
    if ("ADMIN".equals(role)) {
        returnDashboard = "admin-dashboard.jsp";
    }

    String patientName = payment.getPatientName() != null ? payment.getPatientName() : (String) session.getAttribute("patientName");
    String doctorName = payment.getDoctorName() != null ? payment.getDoctorName() : (doc != null ? doc.getName() : "Specialist");
    String doctorSpecialty = payment.getDoctorSpecialization() != null ? payment.getDoctorSpecialization() : (doc != null ? doc.getSpecialization() : "Consultation");
    Object paymentDate = payment.getPaymentDate() != null ? payment.getPaymentDate().toLocalDateTime().toLocalDate() : java.time.LocalDate.now();
    Object consultDate = payment.getAppointmentDate() != null ? payment.getAppointmentDate().toLocalDateTime().toLocalDate() : (appt != null && appt.getAppointmentDate() != null ? appt.getAppointmentDate().toLocalDateTime().toLocalDate() : "");
    String consultTime = appt != null ? appt.getFormattedTime() : payment.getFormattedTime();
    String formattedAmount = String.format(java.util.Locale.US, "%.2f", payment.getAmount());
    String apptStatus = appt != null ? appt.getStatus() : payment.getAppointmentStatus();

    request.setAttribute("payment", payment);
    request.setAttribute("returnDashboard", returnDashboard);
    request.setAttribute("patientName", patientName);
    request.setAttribute("doctorName", doctorName);
    request.setAttribute("doctorSpecialty", doctorSpecialty);
    request.setAttribute("paymentDate", paymentDate);
    request.setAttribute("consultDate", consultDate);
    request.setAttribute("consultTime", consultTime);
    request.setAttribute("formattedAmount", formattedAmount);
    request.setAttribute("apptStatus", apptStatus);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Payment Receipt #PAY-<c:out value="${payment.id}" /> - MediCare ODAS</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        .receipt-card {
            max-width: 680px;
            margin: 30px auto;
            background: #ffffff;
            border-radius: var(--radius-md);
            border: 1px solid var(--border-color);
            box-shadow: var(--shadow-md);
            padding: 36px 40px;
        }
        .receipt-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding-bottom: 20px;
            border-bottom: 2px solid var(--teal-accent);
            margin-bottom: 24px;
        }
        .receipt-brand {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .receipt-meta {
            text-align: right;
            font-size: 0.88rem;
            color: var(--text-muted);
        }
        .receipt-meta strong {
            color: var(--text-primary);
            font-size: 1.05rem;
        }
        .receipt-status-banner {
            background-color: #ecfdf5;
            border: 1px solid #a7f3d0;
            border-radius: var(--radius-sm);
            padding: 14px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 24px;
        }
        .receipt-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 24px;
        }
        .receipt-table th, .receipt-table td {
            padding: 12px 14px;
            text-align: left;
            border-bottom: 1px solid var(--border-color);
            font-size: 0.95rem;
        }
        .receipt-table th {
            width: 38%;
            color: var(--text-muted);
            font-weight: 500;
        }
        .receipt-table td {
            color: var(--text-primary);
            font-weight: 600;
        }
        .receipt-total-row {
            background-color: #f8fafc;
            border-top: 2px solid var(--primary-blue);
        }
        .receipt-total-row td {
            font-size: 1.25rem;
            color: var(--primary-blue);
            font-weight: 800;
        }
        .simulation-receipt-note {
            background-color: #f1f5f9;
            border: 1px dashed #94a3b8;
            border-radius: var(--radius-sm);
            padding: 12px 16px;
            color: #475569;
            font-size: 0.85rem;
            line-height: 1.5;
            text-align: center;
            margin-bottom: 24px;
        }
        .receipt-actions {
            display: flex;
            justify-content: center;
            gap: 16px;
            margin: 24px 0 40px 0;
        }
        @media print {
            header, footer, .receipt-actions, .alert-success {
                display: none !important;
            }
            body {
                background: #ffffff !important;
            }
            .receipt-card {
                border: none !important;
                box-shadow: none !important;
                padding: 0 !important;
                margin: 0 !important;
                max-width: 100% !important;
            }
        }
    </style>
</head>
<body>

    <header>
        <div class="header-container">
            <a href="index.jsp" class="logo">
                <span class="logo-icon">🏥</span>
                <span class="logo-text">MediCare <span>ODAS</span></span>
            </a>
            <nav>
                <a href="<c:out value='${returnDashboard}' />">My Dashboard</a>
                <a href="doctors">Find Doctors</a>
                <c:if test="${sessionScope.role == 'PATIENT'}">
                    <a href="appointment.jsp">Book Appointment</a>
                    <a href="profile">My Profile</a>
                </c:if>
                <a href="change-password.jsp">Change Password</a>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container" style="padding-top: 10px;">
        <c:if test="${param.msg == 'success'}">
            <div class="alert alert-success" style="max-width: 680px; margin: 16px auto 0 auto;">
                ✅ Payment processed successfully! Your consultation has been confirmed.
            </div>
        </c:if>
        <c:if test="${param.msg == 'already_paid'}">
            <div class="alert alert-info" style="max-width: 680px; margin: 16px auto 0 auto;">
                ℹ️ This consultation fee has already been paid in full. Here is your official receipt.
            </div>
        </c:if>

        <div class="receipt-card">
            <%-- Letterhead --%>
            <div class="receipt-header">
                <div class="receipt-brand">
                    <span style="font-size: 2.2rem;">🏥</span>
                    <div>
                        <h2 style="font-size: 1.35rem; margin: 0; color: var(--text-primary);">MediCare ODAS</h2>
                        <p style="margin: 2px 0 0 0; font-size: 0.82rem; color: var(--text-muted);">Official Consultation Fee Receipt</p>
                    </div>
                </div>
                <div class="receipt-meta">
                    <div>Receipt: <strong>#PAY-<c:out value="${payment.id}" /></strong></div>
                    <div>Date: <strong><c:out value="${paymentDate}" /></strong></div>
                </div>
            </div>

            <%-- Success Banner --%>
            <div class="receipt-status-banner">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 1.3rem;">✅</span>
                    <div>
                        <strong style="color: #065f46; font-size: 1.05rem;">Payment Confirmed</strong>
                        <div style="font-size: 0.84rem; color: #047857;">Transaction Status: <c:out value="${payment.paymentStatus}" /></div>
                    </div>
                </div>
                <div>
                    <span class="badge badge-success" style="font-size: 0.9rem; padding: 6px 12px;">PAID</span>
                </div>
            </div>

            <%-- Transaction Details Table --%>
            <table class="receipt-table">
                <tbody>
                    <tr>
                        <th>Receipt Reference</th>
                        <td>#PAY-<c:out value="${payment.id}" /></td>
                    </tr>
                    <tr>
                        <th>Consultation Booking #</th>
                        <td>Ref #<c:out value="${payment.appointmentId}" /></td>
                    </tr>
                    <tr>
                        <th>Patient Name</th>
                        <td>👤 <c:out value="${patientName}" /></td>
                    </tr>
                    <tr>
                        <th>Attending Physician</th>
                        <td>👨‍⚕️ <c:out value="${doctorName}" /></td>
                    </tr>
                    <tr>
                        <th>Medical Specialty</th>
                        <td>🩺 <c:out value="${doctorSpecialty}" /></td>
                    </tr>
                    <tr>
                        <th>Consultation Date &amp; Time</th>
                        <td>
                            🗓️ <c:out value="${consultDate}" /> at <c:out value="${consultTime}" />
                        </td>
                    </tr>
                    <tr>
                        <th>Payment Mode</th>
                        <td><c:out value="${payment.paymentMode}" /> (Simulated)</td>
                    </tr>
                    <tr>
                        <th>Transaction Timestamp</th>
                        <td><c:out value="${payment.paymentDate}" /></td>
                    </tr>
                    <tr class="receipt-total-row">
                        <td style="font-weight: 700; color: var(--text-primary);">Total Amount Paid:</td>
                        <td>₹<c:out value="${formattedAmount}" /></td>
                    </tr>
                </tbody>
            </table>

            <%-- Visible Simulation Disclaimer Note --%>
            <div class="simulation-receipt-note">
                ℹ️ <strong>Demo / simulated payment:</strong> No real money is charged and no payment gateway is used. This receipt serves as proof of consultation fee settlement in MediCare ODAS.
            </div>

            <%-- Cancellation Note if Cancelled after Payment --%>
            <c:if test="${fn:toUpperCase(apptStatus) == 'CANCELLED'}">
                <div class="alert alert-warning" style="margin-top: 14px; font-size: 0.88rem;">
                    ⚠️ <strong>Notice:</strong> This appointment was subsequently cancelled. Paid (refund not processed in demo).
                </div>
            </c:if>
        </div>

        <%-- Print & Navigation Actions --%>
        <div class="receipt-actions">
            <button onclick="window.print();" class="btn btn-primary" style="display: flex; align-items: center; gap: 8px;">
                <span>🖨️</span>
                <span>Print / Download Receipt</span>
            </button>
            <a href="<c:out value='${returnDashboard}' />" class="btn btn-secondary">
                Return to Dashboard
            </a>
        </div>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


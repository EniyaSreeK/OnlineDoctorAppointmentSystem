<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="com.odas.Appointment" %>
<%@ page import="com.odas.Doctor" %>
<%
    // Prevent browser caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String role = (String) session.getAttribute("role");
    if (session.getAttribute("userId") == null || (!"PATIENT".equals(role) && !"ADMIN".equals(role))) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    Appointment appt = (Appointment) request.getAttribute("appointment");
    if (appt == null) {
        response.sendRedirect("patient-dashboard.jsp");
        return;
    }

    Doctor doc = (Doctor) request.getAttribute("doctor");
    Double fee = (Double) request.getAttribute("consultationFee");
    if (fee == null) fee = 500.0;

    String patientName = (String) session.getAttribute("patientName");
    if (patientName == null) patientName = (String) session.getAttribute("username");

    String displayDoctorName = (doc != null && doc.getName() != null) ? doc.getName() : appt.getDoctorName();
    String displayDoctorSpecialty = (doc != null && doc.getSpecialization() != null) ? doc.getSpecialization() : appt.getDoctorSpecialization();
    Object apptDate = appt.getAppointmentDate() != null ? appt.getAppointmentDate().toLocalDateTime().toLocalDate() : "";
    String formattedFee = String.format(java.util.Locale.US, "%.2f", fee);
    String sanitizedPatientUsername = patientName.toLowerCase().replaceAll("\\s+", "");

    request.setAttribute("appt", appt);
    request.setAttribute("doc", doc);
    request.setAttribute("patientName", patientName);
    request.setAttribute("displayDoctorName", displayDoctorName);
    request.setAttribute("displayDoctorSpecialty", displayDoctorSpecialty);
    request.setAttribute("apptDate", apptDate);
    request.setAttribute("formattedFee", formattedFee);
    request.setAttribute("sanitizedPatientUsername", sanitizedPatientUsername);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Consultation Checkout - MediCare ODAS</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        .checkout-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 24px;
            margin-top: 20px;
        }
        @media (max-width: 768px) {
            .checkout-grid {
                grid-template-columns: 1fr;
            }
        }
        .order-summary-box {
            background-color: #f8fafc;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            padding: 24px;
        }
        .order-row {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px dashed var(--border-color);
            font-size: 0.95rem;
        }
        .order-total-row {
            display: flex;
            justify-content: space-between;
            padding: 16px 0 0 0;
            font-size: 1.25rem;
            font-weight: 800;
            color: var(--primary-blue);
        }
        .simulation-banner {
            background-color: #eff6ff;
            border: 1px solid #bfdbfe;
            border-left: 5px solid var(--primary-blue);
            color: #1e40af;
            padding: 14px 18px;
            border-radius: var(--radius-sm);
            margin-bottom: 24px;
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 0.92rem;
            line-height: 1.5;
        }
        .mode-details-card {
            background-color: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-sm);
            padding: 16px;
            margin-top: 14px;
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
                <a href="patient-dashboard.jsp">My Dashboard</a>
                <a href="doctors">Find Doctors</a>
                <a href="appointment.jsp">Book Appointment</a>
                <a href="profile">My Profile</a>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge">👤 <c:out value="${patientName}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container">
        <div class="auth-wrapper" style="padding: 24px 0;">
            <div class="auth-card" style="max-width: 840px;">
                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
                    <span style="font-size: 2.2rem;">💳</span>
                    <div>
                        <h2 style="margin-bottom: 2px;">Consultation Fee Checkout</h2>
                        <p class="auth-subtitle" style="margin-bottom: 0;">Complete simulated online payment to confirm your specialist consultation.</p>
                    </div>
                </div>

                <%-- Prominent Simulation Disclaimer Note --%>
                <div class="simulation-banner" style="margin-top: 16px;">
                    <span style="font-size: 1.4rem;">ℹ️</span>
                    <div>
                        <strong>Demo / simulated payment:</strong> No real money is charged and no payment gateway is used. Your appointment payment status will be updated securely in the system.
                    </div>
                </div>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger" style="margin-bottom: 20px;">
                        ⚠️ <c:out value="${errorMessage}" />
                    </div>
                </c:if>

                <div class="checkout-grid">
                    <%-- Left Column: Consultation Order Summary --%>
                    <div class="order-summary-box">
                        <h3 style="font-size: 1.1rem; margin-bottom: 16px; color: var(--text-primary);">
                            Consultation Summary
                        </h3>

                        <div class="order-row">
                            <span class="text-muted">Booking Reference</span>
                            <strong>#<c:out value="${appt.id}" /></strong>
                        </div>
                        <div class="order-row">
                            <span class="text-muted">Attending Doctor</span>
                            <span>👨‍⚕️ <c:out value="${displayDoctorName}" /></span>
                        </div>
                        <div class="order-row">
                            <span class="text-muted">Specialization</span>
                            <span>🩺 <c:out value="${displayDoctorSpecialty}" /></span>
                        </div>
                        <div class="order-row">
                            <span class="text-muted">Appointment Date</span>
                            <span>🗓️ <c:out value="${apptDate}" /></span>
                        </div>
                        <div class="order-row">
                            <span class="text-muted">Scheduled Time</span>
                            <span>⏰ <c:out value="${appt.formattedTime}" /></span>
                        </div>
                        <div class="order-row">
                            <span class="text-muted">Patient Name</span>
                            <span>👤 <c:out value="${patientName}" /></span>
                        </div>

                        <div class="order-total-row">
                            <span>Amount Due:</span>
                            <span>₹<c:out value="${formattedFee}" /></span>
                        </div>
                    </div>

                    <%-- Right Column: Payment Mode Selection --%>
                    <div>
                        <form action="payment" method="post" class="auth-form" autocomplete="off" onsubmit="return validatePayment();">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="appointmentId" value="<c:out value='${appt.id}' />">

                            <div class="form-group">
                                <label for="paymentMode">Select Payment Method <span style="color: var(--danger-color);">*</span></label>
                                <select id="paymentMode" name="paymentMode" required onchange="onPaymentModeChange(this.value);" style="width: 100%; padding: 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 0.95rem;">
                                    <option value="">-- Choose Payment Mode --</option>
                                    <option value="Card">💳 Credit / Debit Card (Simulated)</option>
                                    <option value="UPI">📱 UPI / Virtual Payment Address (Simulated)</option>
                                    <option value="Net Banking">🏦 Net Banking (Simulated)</option>
                                </select>
                            </div>

                            <%-- Dynamic Simulated Fields (No real card details asked or stored) --%>
                            <div id="cardFields" class="mode-details-card" style="display: none;">
                                <div class="form-group" style="margin-bottom: 12px;">
                                    <label for="cardHolder">Name on Card</label>
                                    <input type="text" id="cardHolder" placeholder="e.g. <c:out value='${patientName}' />" value="<c:out value='${patientName}' />">
                                </div>
                                <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 12px;">
                                    <div class="form-group" style="margin-bottom: 0;">
                                        <label for="cardSimNum">Card Number (Demo)</label>
                                        <input type="text" id="cardSimNum" placeholder="•••• •••• •••• 4242" value="4111 •••• •••• 4242" readonly style="background: #f1f5f9; color: var(--text-muted);">
                                    </div>
                                    <div class="form-group" style="margin-bottom: 0;">
                                        <label for="cardSimExp">Expiry</label>
                                        <input type="text" id="cardSimExp" placeholder="12/28" value="12/28" readonly style="background: #f1f5f9; color: var(--text-muted);">
                                    </div>
                                </div>
                            </div>

                            <div id="upiFields" class="mode-details-card" style="display: none;">
                                <div class="form-group" style="margin-bottom: 0;">
                                    <label for="upiId">UPI ID / VPA</label>
                                    <input type="text" id="upiId" placeholder="e.g. <c:out value='${sanitizedPatientUsername}' />@okhdfcbank" value="<c:out value='${sanitizedPatientUsername}' />@upi">
                                </div>
                            </div>

                            <div id="netBankingFields" class="mode-details-card" style="display: none;">
                                <div class="form-group" style="margin-bottom: 0;">
                                    <label for="bankSelect">Select Bank</label>
                                    <select id="bankSelect" style="width: 100%; padding: 10px; border: 1px solid var(--border-color); border-radius: var(--radius-sm);">
                                        <option value="HDFC">HDFC Bank</option>
                                        <option value="SBI">State Bank of India (SBI)</option>
                                        <option value="ICICI">ICICI Bank</option>
                                        <option value="AXIS">Axis Bank</option>
                                        <option value="KOTAK">Kotak Mahindra Bank</option>
                                    </select>
                                </div>
                            </div>

                            <div style="margin-top: 24px;">
                                <button type="submit" class="btn btn-primary btn-block" style="font-size: 1.05rem; padding: 14px;">
                                    Pay ₹<c:out value="${formattedFee}" /> (Demo Payment)
                                </button>
                                <a href="patient-dashboard.jsp" class="btn btn-secondary btn-block" style="text-decoration: none; text-align: center; margin-top: 10px;">
                                    Cancel &amp; Return to Dashboard
                                </a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

    <script>
        function onPaymentModeChange(val) {
            document.getElementById('cardFields').style.display = (val === 'Card') ? 'block' : 'none';
            document.getElementById('upiFields').style.display = (val === 'UPI') ? 'block' : 'none';
            document.getElementById('netBankingFields').style.display = (val === 'Net Banking') ? 'block' : 'none';
        }

        function validatePayment() {
            var mode = document.getElementById('paymentMode').value;
            if (!mode) {
                alert('Please select a payment mode.');
                return false;
            }
            return true;
        }
    </script>

</body>
</html>


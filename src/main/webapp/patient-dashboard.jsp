<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    // Verify controller invocation; forward to PatientDashboardServlet if directly accessed
    if (request.getAttribute("allAppts") == null) {
        request.getRequestDispatcher("/patient-dashboard").forward(request, response);
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Patient Portal - MediCare ODAS</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>

    <header>
        <div class="header-container">
            <a href="index.jsp" class="logo">
                <span class="logo-icon">🏥</span>
                <span class="logo-text">MediCare <span>ODAS</span></span>
            </a>
            <nav>
                <a href="patient-dashboard.jsp" class="active">My Dashboard</a>
                <a href="doctors">Find Doctors</a>
                <a href="appointment.jsp">Book Appointment</a>
                <a href="profile">My Profile</a>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge">👤 <c:out value="${patientName}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="dashboard-container">
        <div class="dashboard-header">
            <h2>Patient Health Dashboard</h2>
            <p>Welcome back, <strong><c:out value="${patientName}" /></strong>! Review your upcoming visits and medical history.</p>
        </div>

        <%-- Status Alerts --%>
        <c:choose>
            <c:when test="${param.msg == 'booked'}">
                <div class="alert alert-success" style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                    <div>
                        ✅ Appointment successfully scheduled! <c:if test="${not empty param.id}">Booking Reference: #<c:out value="${param.id}" /></c:if>
                    </div>
                    <c:if test="${not empty param.id}">
                        <a href="payment?appointmentId=<c:out value='${param.id}' />" class="btn btn-primary btn-sm" style="white-space: nowrap;">
                            💳 Pay Consultation Fee Now
                        </a>
                    </c:if>
                </div>
            </c:when>
            <c:when test="${param.msg == 'rescheduled'}">
                <div class="alert alert-success">
                    ✅ Appointment <c:if test="${not empty param.id}">#<c:out value="${param.id}" /></c:if> successfully rescheduled to your new date & time slot!
                </div>
            </c:when>
            <c:when test="${param.msg == 'cancelled'}">
                <div class="alert alert-info">
                    ℹ️ The appointment has been successfully cancelled.
                </div>
            </c:when>
            <c:when test="${not empty param.error}">
                <div class="alert alert-danger">
                    ⚠️ <c:out value="${param.error}" />
                </div>
            </c:when>
        </c:choose>

        <%-- STAGE 4: 24-Hour In-App Reminder Banner --%>
        <c:if test="${not empty reminderAppts}">
            <c:forEach var="rem" items="${reminderAppts}">
                <div class="alert alert-warning" style="background-color: #fef3c7; border: 1px solid #f59e0b; border-left: 6px solid #d97706; color: #92400e; padding: 14px 18px; border-radius: var(--radius-sm); margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; box-shadow: var(--shadow-sm);">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <span style="font-size: 1.5rem;">🔔</span>
                        <div>
                            <strong style="color: #b45309;">Upcoming Consultation Reminder:</strong> You have an appointment with 
                            <strong><c:out value="${rem.doctorName}" /></strong> (<c:out value="${rem.doctorSpecialization}" />) 
                            scheduled on <strong><c:out value="${rem.appointmentDate.toLocalDateTime().toLocalDate()}" /></strong> 
                            at <strong><c:out value="${rem.formattedTime}" /></strong> (within the next 24 hours).
                        </div>
                    </div>
                    <div>
                        <a href="appointment.jsp?rescheduleId=<c:out value='${rem.id}' />" class="btn btn-sm btn-secondary" style="background: #ffffff; color: #92400e; border: 1px solid #d97706; white-space: nowrap;">
                            Reschedule
                        </a>
                    </div>
                </div>
            </c:forEach>
        </c:if>

        <div class="dashboard-actions">
            <a href="appointment.jsp" class="btn btn-primary">Book New Consultation</a>
            <a href="doctors" class="btn btn-secondary">Browse Specialists</a>
        </div>

        <%-- Metrics --%>
        <div class="metrics-grid">
            <div class="metric-card">
                <h4>Upcoming Consultations</h4>
                <div class="metric-value"><c:out value="${fn:length(upcomingAppts)}" /></div>
            </div>
            <div class="metric-card">
                <h4>Past Consultations</h4>
                <div class="metric-value" style="color: var(--teal-hover);"><c:out value="${fn:length(pastAppts)}" /></div>
            </div>
            <div class="metric-card">
                <h4>Total Bookings</h4>
                <div class="metric-value" style="color: var(--text-primary);"><c:out value="${fn:length(allAppts)}" /></div>
            </div>
        </div>

        <%-- Upcoming Consultations --%>
        <section class="dashboard-card">
            <h3>Upcoming Consultations (<c:out value="${fn:length(upcomingAppts)}" />)</h3>
            <c:choose>
                <c:when test="${not empty upcomingAppts}">
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Ref #</th>
                                    <th>Doctor</th>
                                    <th>Specialization</th>
                                    <th>Scheduled Date & Time</th>
                                    <th>Fee / Payment</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="appt" items="${upcomingAppts}">
                                    <tr>
                                        <td><strong>#<c:out value="${appt.id}" /></strong></td>
                                        <td>👨‍⚕️ <c:out value="${appt.doctorName}" /></td>
                                        <td><span class="specialization-badge">🩺 <c:out value="${appt.doctorSpecialization}" /></span></td>
                                        <td><c:out value="${appt.appointmentDate.toLocalDateTime().toLocalDate()}" /> at <c:out value="${appt.formattedTime}" /></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${fn:toUpperCase(appt.paymentStatus) == 'PAID'}">
                                                    <a href="payment?action=receipt&appointmentId=<c:out value='${appt.id}' />" 
                                                       class="btn btn-secondary btn-sm" style="color: #065f46; border-color: #a7f3d0; background: #ecfdf5; white-space: nowrap;">
                                                        🧾 Paid (Receipt)
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <a href="payment?appointmentId=<c:out value='${appt.id}' />" 
                                                       class="btn btn-primary btn-sm" style="white-space: nowrap;">
                                                        💳 Pay Now
                                                    </a>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div style="display: flex; gap: 8px;">
                                                <a href="appointment.jsp?rescheduleId=<c:out value='${appt.id}' />" 
                                                   class="btn btn-secondary btn-sm" style="white-space: nowrap;">
                                                    Reschedule
                                                </a>
                                                <form action="appointment-action" method="post" style="display:inline;" onsubmit="return confirm('Are you sure you want to cancel your consultation with ${fn:escapeXml(appt.doctorName)}?');">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="cancel">
                                                    <input type="hidden" name="id" value="<c:out value='${appt.id}' />">
                                                    <button type="submit" class="btn btn-danger btn-sm" style="white-space: nowrap;">Cancel</button>
                                                </form>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <div class="empty-icon">📅</div>
                        <p>No upcoming appointments scheduled.</p>
                        <a href="appointment.jsp" class="btn btn-primary btn-sm" style="margin-top: 14px;">Schedule Your First Consultation</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- Past Appointments --%>
        <section class="dashboard-card">
            <h3>Past & Cancelled Appointments (<c:out value="${fn:length(pastAppts)}" />)</h3>
            <c:choose>
                <c:when test="${not empty pastAppts}">
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Ref #</th>
                                    <th>Doctor</th>
                                    <th>Specialization</th>
                                    <th>Date & Time</th>
                                    <th>Status</th>
                                    <th>Billing / Receipt</th>
                                    <th>Prescription</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="appt" items="${pastAppts}">
                                    <c:set var="badgeClass" value="badge-secondary" />
                                    <c:if test="${appt.status == 'COMPLETED'}">
                                        <c:set var="badgeClass" value="badge-primary" />
                                    </c:if>
                                    <c:if test="${appt.status == 'CANCELLED'}">
                                        <c:set var="badgeClass" value="badge-danger" />
                                    </c:if>
                                    <c:set var="rx" value="${prescriptionMap[appt.id]}" />
                                    <c:set var="isPaid" value="${fn:toUpperCase(appt.paymentStatus) == 'PAID'}" />
                                    <c:set var="isCancelled" value="${fn:toUpperCase(appt.status) == 'CANCELLED'}" />
                                    <tr>
                                        <td><strong>#<c:out value="${appt.id}" /></strong></td>
                                        <td>👨‍⚕️ <c:out value="${appt.doctorName}" /></td>
                                        <td><span class="specialization-badge">🩺 <c:out value="${appt.doctorSpecialization}" /></span></td>
                                        <td><c:out value="${appt.appointmentDate.toLocalDateTime().toLocalDate()}" /> at <c:out value="${appt.formattedTime}" /></td>
                                        <td>
                                            <span class="badge ${badgeClass}"><c:out value="${appt.status}" /></span>
                                            <c:if test="${isCancelled && (isPaid || fn:toUpperCase(appt.paymentStatus) == 'REFUNDED')}">
                                                <div style="font-size: 0.78rem; color: #b45309; margin-top: 4px; font-weight: 600;">
                                                    ⚠️ Refunded
                                                </div>
                                            </c:if>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${isPaid || fn:toUpperCase(appt.paymentStatus) == 'REFUNDED'}">
                                                    <a href="payment?action=receipt&appointmentId=<c:out value='${appt.id}' />" 
                                                       class="btn btn-secondary btn-sm" style="white-space: nowrap;">
                                                        🧾 View Receipt
                                                    </a>
                                                </c:when>
                                                <c:when test="${appt.status == 'COMPLETED' && fn:toUpperCase(appt.paymentStatus) == 'UNPAID'}">
                                                    <a href="payment?appointmentId=<c:out value='${appt.id}' />" 
                                                       class="btn btn-primary btn-sm" style="white-space: nowrap;">
                                                        💳 Pay Now
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted" style="font-size: 0.85rem;"><c:out value="${appt.paymentStatus}" /></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty rx}">
                                                    <a href="prescription?appointmentId=<c:out value='${appt.id}' />" 
                                                       class="btn btn-secondary btn-sm" style="white-space: nowrap;">
                                                        📄 View Prescription
                                                    </a>
                                                </c:when>
                                                <c:when test="${appt.status == 'COMPLETED'}">
                                                    <span class="text-muted" style="font-size: 0.85rem;">Pending Rx</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted" style="font-size: 0.85rem;">&mdash;</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <p>No past appointments recorded yet.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


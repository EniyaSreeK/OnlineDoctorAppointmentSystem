<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    // Verify controller invocation; forward to DoctorDashboardServlet if directly accessed
    if (request.getAttribute("doctorAppts") == null) {
        request.getRequestDispatcher("/doctor-dashboard").forward(request, response);
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Physician Portal - MediCare ODAS</title>
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
                <a href="doctor-dashboard.jsp" class="active">Doctor Portal</a>
                <a href="doctors">Doctors Directory</a>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge">👨‍⚕️ <c:out value="${doctorName}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="dashboard-container">
        <div class="dashboard-header">
            <h2>Physician Portal</h2>
            <p>Welcome, <strong><c:out value="${doctorName}" /></strong> (<c:out value="${specialization}" />). Manage your consultation schedule.</p>
        </div>

        <c:choose>
            <c:when test="${param.msg == 'completed'}">
                <div class="alert alert-success">
                    ✅ Consultation marked as completed successfully.
                </div>
            </c:when>
            <c:when test="${param.msg == 'cancelled'}">
                <div class="alert alert-info">
                    ℹ️ Consultation has been cancelled.
                </div>
            </c:when>
            <c:when test="${param.msg == 'created' || param.msg == 'prescription_saved'}">
                <div class="alert alert-success">
                    ✅ Prescription successfully recorded and issued to patient.
                </div>
            </c:when>
            <c:when test="${param.msg == 'already_exists'}">
                <div class="alert alert-info">
                    ℹ️ A prescription has already been issued for this consultation.
                </div>
            </c:when>
            <c:when test="${param.msg == 'declined' || param.msg == 'cancelled'}">
                <div class="alert alert-success">
                    ✅ Consultation appointment declined / cancelled successfully. The patient has been notified and the slot is open.
                </div>
            </c:when>
            <c:when test="${param.msg == 'schedule_updated'}">
                <div class="alert alert-success">
                    ✅ Consultation schedule updated successfully.
                </div>
            </c:when>
            <c:when test="${not empty param.error}">
                <div class="alert alert-danger">
                    ⚠️ <c:out value="${param.error}" />
                </div>
            </c:when>
        </c:choose>

        <%-- Metrics --%>
        <div class="metrics-grid">
            <div class="metric-card">
                <h4>Pending Consultations</h4>
                <div class="metric-value"><c:out value="${scheduledCount}" /></div>
            </div>
            <div class="metric-card">
                <h4>Completed Consultations</h4>
                <div class="metric-value" style="color: var(--teal-hover);"><c:out value="${completedCount}" /></div>
            </div>
            <div class="metric-card">
                <h4>Total Consultations</h4>
                <div class="metric-value" style="color: var(--text-primary);"><c:out value="${fn:length(doctorAppts)}" /></div>
            </div>
        </div>

        <%-- Doctor Availability Schedule Management --%>
        <section class="dashboard-card" style="margin-bottom: 24px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                <h3>📅 My Consultation Schedule</h3>
            </div>
            <form action="doctor-schedule" method="post" style="display: grid; grid-template-columns: 1fr 1fr auto; gap: 16px; align-items: end;">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <div class="form-group" style="margin-bottom: 0;">
                    <label for="availableDays">Available Days (e.g. Mon,Wed,Fri)</label>
                    <input type="text" id="availableDays" name="availableDays" required 
                           value="<c:out value='${currentDoctor.availableDays}' />" 
                           placeholder="e.g. Mon,Wed,Fri">
                </div>
                <div class="form-group" style="margin-bottom: 0;">
                    <label for="availableTime">Available Time (24-hr HH:mm-HH:mm)</label>
                    <input type="text" id="availableTime" name="availableTime" required pattern="([01]\d|2[0-3]):([0-5]\d)-([01]\d|2[0-3]):([0-5]\d)"
                           value="<c:out value='${currentDoctor.availableTime}' />" 
                           placeholder="e.g. 09:00-13:00">
                </div>
                <button type="submit" class="btn btn-primary" style="height: 42px;">Update Schedule</button>
            </form>
        </section>

        <%-- Patient History View (Filtered to THIS Doctor Only) --%>
        <c:if test="${not empty param.patientHistoryId}">
            <section class="dashboard-card" style="margin-bottom: 24px; border-left: 4px solid var(--teal);">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                    <h3>📋 Patient Medical History: <c:out value="${historyPatientName}" /> (#PAT-<c:out value="${param.patientHistoryId}" />)</h3>
                    <a href="doctor-dashboard.jsp" class="btn btn-secondary btn-sm">✕ Close History</a>
                </div>
                <c:choose>
                    <c:when test="${not empty patientHistoryAppts}">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Appt #</th>
                                        <th>Date & Time</th>
                                        <th>Status</th>
                                        <th>Diagnosis</th>
                                        <th>Prescription Details</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="ha" items="${patientHistoryAppts}">
                                        <tr>
                                            <td><strong>#<c:out value="${ha.id}" /></strong></td>
                                            <td><c:out value="${ha.appointmentDate.toLocalDateTime().toLocalDate()}" /> at <c:out value="${ha.formattedTime}" /></td>
                                            <td><span class="badge ${ha.status == 'COMPLETED' ? 'badge-success' : (ha.status == 'CANCELLED' ? 'badge-danger' : 'badge-primary')}"><c:out value="${ha.status}" /></span></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty patientHistoryRx[ha.id]}">
                                                        <c:out value="${patientHistoryRx[ha.id].diagnosis}" />
                                                    </c:when>
                                                    <c:otherwise><span class="text-muted">No diagnosis</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty patientHistoryRx[ha.id]}">
                                                        <a href="prescription?appointmentId=<c:out value='${ha.id}' />" class="btn btn-secondary btn-sm">📄 View Rx</a>
                                                    </c:when>
                                                    <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <p class="text-muted">No past appointments found for this patient with your practice.</p>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:if>

        <section class="dashboard-card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px;">
                <h3>Assigned Patient Consultations</h3>
            </div>

            <c:choose>
                <c:when test="${not empty doctorAppts}">
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Ref #</th>
                                    <th>Patient Name</th>
                                    <th>Scheduled Date & Time</th>
                                    <th>Status</th>
                                    <th>Clinical Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="appt" items="${doctorAppts}">
                                    <c:set var="badgeClass" value="badge-primary" />
                                    <c:if test="${appt.status == 'COMPLETED'}">
                                        <c:set var="badgeClass" value="badge-success" />
                                    </c:if>
                                    <c:if test="${appt.status == 'CANCELLED'}">
                                        <c:set var="badgeClass" value="badge-danger" />
                                    </c:if>
                                    <tr>
                                        <td><strong>#<c:out value="${appt.id}" /></strong></td>
                                        <td>👤 <c:out value="${appt.patientName}" /></td>
                                        <td><c:out value="${appt.appointmentDate.toLocalDateTime().toLocalDate()}" /> at <c:out value="${appt.formattedTime}" /></td>
                                        <td><span class="badge ${badgeClass}"><c:out value="${appt.status}" /></span></td>
                                        <td>
                                            <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                                                <a href="doctor-dashboard.jsp?patientHistoryId=<c:out value='${appt.patientId}' />" 
                                                   class="btn btn-secondary btn-sm" style="white-space: nowrap;">
                                                    📋 History
                                                </a>
                                                <c:choose>
                                                    <c:when test="${appt.status == 'SCHEDULED'}">
                                                        <form action="appointment-action" method="post" style="display:inline;">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                            <input type="hidden" name="action" value="complete">
                                                            <input type="hidden" name="id" value="<c:out value='${appt.id}' />">
                                                            <button type="submit" class="btn btn-success btn-sm" style="white-space: nowrap;">Mark Completed</button>
                                                        </form>
                                                        <form action="appointment-action" method="post" style="display:inline;" onsubmit="return confirm('Are you sure you want to decline / cancel this consultation with ${fn:escapeXml(appt.patientName)}?');">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                            <input type="hidden" name="action" value="cancel">
                                                            <input type="hidden" name="id" value="<c:out value='${appt.id}' />">
                                                            <button type="submit" class="btn btn-danger btn-sm" style="white-space: nowrap;">Decline</button>
                                                        </form>
                                                    </c:when>
                                                    <c:when test="${appt.status == 'COMPLETED'}">
                                                        <c:set var="rx" value="${prescriptionMap[appt.id]}" />
                                                        <c:choose>
                                                            <c:when test="${not empty rx}">
                                                                <a href="prescription?appointmentId=<c:out value='${appt.id}' />" 
                                                                   class="btn btn-secondary btn-sm" style="white-space: nowrap;">
                                                                    📄 View Rx
                                                                </a>
                                                                <a href="prescription?action=edit&appointmentId=<c:out value='${appt.id}' />" 
                                                                   class="btn btn-primary btn-sm" style="white-space: nowrap;">
                                                                    ✏️ Edit Rx
                                                                </a>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <a href="prescription?action=add&appointmentId=<c:out value='${appt.id}' />" 
                                                                   class="btn btn-primary btn-sm" style="white-space: nowrap;">
                                                                    💊 Add Rx
                                                                </a>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-muted"><c:out value="${appt.status}" /></span>
                                                    </c:otherwise>
                                                </c:choose>
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
                        <div class="empty-icon">🩺</div>
                        <p>No patient consultations currently assigned to your schedule.</p>
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


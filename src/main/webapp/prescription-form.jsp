<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="com.odas.Appointment" %>
<%
    // Prevent browser caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String role = (String) session.getAttribute("role");
    if (session.getAttribute("userId") == null || (!"DOCTOR".equals(role) && !"ADMIN".equals(role))) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    Appointment appt = (Appointment) request.getAttribute("appointment");
    if (appt == null) {
        response.sendRedirect("doctor-dashboard.jsp");
        return;
    }

    String doctorName = (String) session.getAttribute("doctorName");
    if (doctorName == null) doctorName = (String) session.getAttribute("username");

    request.setAttribute("role", role);
    request.setAttribute("doctorName", doctorName);
    request.setAttribute("appt", appt);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Issue Medical Prescription - MediCare ODAS</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://gstatic.com" crossorigin>
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
                <c:choose>
                    <c:when test="${role == 'DOCTOR'}">
                        <a href="doctor-dashboard.jsp" class="active">Doctor Portal</a>
                    </c:when>
                    <c:otherwise>
                        <a href="admin-dashboard.jsp" class="active">Admin Dashboard</a>
                    </c:otherwise>
                </c:choose>
                <a href="doctors">Doctors Directory</a>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge"><c:out value="${role}" />: <c:out value="${doctorName}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container">
        <div class="auth-wrapper" style="padding: 24px 0;">
            <div class="auth-card" style="max-width: 650px;">
                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
                    <span style="font-size: 2rem;">💊</span>
                    <div>
                        <h2 style="margin-bottom: 2px;">
                            <c:choose>
                                <c:when test="${isEdit}">Update Medical Prescription</c:when>
                                <c:otherwise>Issue Clinical Prescription</c:otherwise>
                            </c:choose>
                        </h2>
                        <p class="auth-subtitle" style="margin-bottom: 0;">
                            <c:choose>
                                <c:when test="${isEdit}">Modify diagnosis and pharmaceutical regimen for patient record.</c:when>
                                <c:otherwise>Prescribe diagnosis and pharmaceutical regimen for completed consultation.</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger" style="margin-top: 16px;">
                        ⚠️ <c:out value="${errorMessage}" />
                    </div>
                </c:if>

                <%-- Consultation Summary Card --%>
                <div style="background-color: var(--teal-light); border: 1px solid rgba(13, 148, 136, 0.25); border-radius: var(--radius-sm); padding: 16px; margin: 20px 0 24px 0;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 0.9rem; color: var(--text-primary);">
                        <div>
                            <span style="color: var(--text-muted); font-size: 0.82rem; display: block;">PATIENT NAME</span>
                            <strong>👤 <c:out value="${appt.patientName}" /></strong>
                        </div>
                        <div>
                            <span style="color: var(--text-muted); font-size: 0.82rem; display: block;">BOOKING REFERENCE</span>
                            <strong>Ref #<c:out value="${appt.id}" /></strong>
                        </div>
                        <div>
                            <span style="color: var(--text-muted); font-size: 0.82rem; display: block;">ATTENDING PHYSICIAN</span>
                            <span>👨‍⚕️ <c:out value="${appt.doctorName}" /> (<c:out value="${appt.doctorSpecialization}" />)</span>
                        </div>
                        <div>
                            <span style="color: var(--text-muted); font-size: 0.82rem; display: block;">CONSULTATION DATE & TIME</span>
                            <span>🗓️ <c:out value="${appt.appointmentDate.toLocalDateTime().toLocalDate()}" /> at <c:out value="${appt.formattedTime}" /></span>
                        </div>
                    </div>
                </div>

                <c:set var="valDiagnosis" value="${not empty prescription ? prescription.diagnosis : param.diagnosis}" />
                <c:set var="valDetails" value="${not empty prescription ? prescription.prescriptionDetails : param.prescriptionDetails}" />

                <form action="prescription" method="post" class="auth-form" autocomplete="off">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="appointmentId" value="<c:out value='${appt.id}' />">
                    <c:if test="${isEdit}">
                        <input type="hidden" name="action" value="edit">
                        <input type="hidden" name="prescriptionId" value="<c:out value='${prescription.id}' />">
                    </c:if>

                    <div class="form-group">
                        <label for="diagnosis">Clinical Diagnosis <span style="color: var(--danger-color);">*</span></label>
                        <input type="text" id="diagnosis" name="diagnosis" required 
                               placeholder="e.g. Acute Upper Respiratory Tract Infection, Stage 1 Hypertension"
                               value="<c:out value='${valDiagnosis}' />">
                    </div>

                    <div class="form-group">
                        <label for="prescriptionDetails">Rx &ndash; Medication, Dosage & Clinical Instructions <span style="color: var(--danger-color);">*</span></label>
                        <textarea id="prescriptionDetails" name="prescriptionDetails" rows="7" required 
                                  placeholder="Example:&#10;1. Tab. Amoxicillin 500mg - 1 capsule three times daily after meals for 5 days&#10;2. Tab. Paracetamol 650mg - 1 tablet SOS for body pain/fever&#10;3. Syp. Diphenhydramine 10ml - At bedtime for 3 days&#10;&#10;Advice: Drink warm fluids, avoid cold water, review in 5 days if fever persists."
                                  style="width: 100%; padding: 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-family: inherit; font-size: 0.95rem; line-height: 1.5; resize: vertical;"><c:out value="${valDetails}" /></textarea>
                    </div>

                    <div style="display: flex; gap: 12px; margin-top: 10px;">
                        <button type="submit" class="btn btn-primary btn-block">
                            <c:choose>
                                <c:when test="${isEdit}">Update Prescription</c:when>
                                <c:otherwise>Save &amp; Issue Prescription</c:otherwise>
                            </c:choose>
                        </button>
                        <a href="doctor-dashboard.jsp" class="btn btn-secondary btn-block" style="text-decoration: none; text-align: center;">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


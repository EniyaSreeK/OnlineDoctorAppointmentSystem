<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="com.odas.Patient" %>
<%@ page import="com.odas.dao.PatientDAO" %>
<%
    // Prevent browser caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    // Authentication check
    String role = (String) session.getAttribute("role");
    Integer userId = (Integer) session.getAttribute("userId");
    if (userId == null || !"PATIENT".equals(role)) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    Patient patient = (Patient) request.getAttribute("patient");
    if (patient == null) {
        PatientDAO patientDAO = new PatientDAO();
        patient = patientDAO.getPatientByUserId(userId);
        request.setAttribute("patient", patient);
    }

    String patientName = (patient != null && patient.getName() != null) ? patient.getName() : (String) session.getAttribute("username");
    request.setAttribute("patientName", patientName);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>My Profile - MediCare ODAS</title>
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
                <a href="patient-dashboard.jsp">My Dashboard</a>
                <a href="doctors">Find Doctors</a>
                <a href="appointment.jsp">Book Appointment</a>
                <a href="profile" class="active">My Profile</a>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge">👤 <c:out value="${patientName}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container">
        <div class="auth-wrapper" style="padding: 20px 0;">
            <div class="auth-card" style="max-width: 580px;">
                <h2>Patient Profile Management</h2>
                <p class="auth-subtitle">View and update your personal demographic and contact details.</p>

                <c:if test="${param.msg == 'updated'}">
                    <div class="alert alert-success">
                        ✅ Your profile details have been updated successfully!
                    </div>
                </c:if>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger">
                        ⚠️ <c:out value="${errorMessage}" />
                    </div>
                </c:if>

                <form action="profile" method="post" class="auth-form" autocomplete="off">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <div class="form-group">
                        <label for="name">Full Name <span style="color: var(--danger-color);">*</span></label>
                        <input type="text" id="name" name="name" required 
                               value="<c:out value='${patient.name}' />"
                               placeholder="e.g. John Doe">
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                        <div class="form-group">
                            <label for="age">Age (Years) <span style="color: var(--danger-color);">*</span></label>
                            <input type="number" id="age" name="age" required min="1" max="120" step="1"
                                   value="${patient.age > 0 ? patient.age : ''}"
                                   placeholder="1 - 120">
                        </div>

                        <div class="form-group">
                            <label for="gender">Gender <span style="color: var(--danger-color);">*</span></label>
                            <select id="gender" name="gender" required>
                                <option value="" ${empty patient.gender ? 'selected' : ''}>-- Select Gender --</option>
                                <option value="Male" ${fn:toUpperCase(fn:trim(patient.gender)) == 'MALE' ? 'selected' : ''}>Male</option>
                                <option value="Female" ${fn:toUpperCase(fn:trim(patient.gender)) == 'FEMALE' ? 'selected' : ''}>Female</option>
                                <option value="Other" ${fn:toUpperCase(fn:trim(patient.gender)) == 'OTHER' ? 'selected' : ''}>Other</option>
                            </select>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                        <div class="form-group">
                            <label for="phone">Phone (10 Digits) <span style="color: var(--danger-color);">*</span></label>
                            <input type="tel" id="phone" name="phone" required pattern="[0-9]{10}"
                                   value="<c:out value='${patient.phone}' />"
                                   placeholder="e.g. 9876543210">
                        </div>

                        <div class="form-group">
                            <label for="email">Email Address <span style="color: var(--danger-color);">*</span></label>
                            <input type="email" id="email" name="email" required
                                   value="<c:out value='${patient.email}' />"
                                   placeholder="e.g. patient@example.com">
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="address">Residential Address</label>
                        <textarea id="address" name="address" rows="3" 
                                  style="width: 100%; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-family: inherit; font-size: 0.95rem;"
                                  placeholder="House/Street, City, Postal Code"><c:out value="${patient.address}" /></textarea>
                    </div>

                    <div style="display: flex; gap: 12px; margin-top: 10px;">
                        <button type="submit" class="btn btn-primary btn-block">Save Profile Changes</button>
                        <a href="patient-dashboard.jsp" class="btn btn-secondary btn-block" style="text-decoration: none; text-align: center;">Back to Dashboard</a>
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


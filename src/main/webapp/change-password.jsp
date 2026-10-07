<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    // Prevent browser caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    // Authentication check for any authenticated role
    Integer userId = (Integer) session.getAttribute("userId");
    String role = (String) session.getAttribute("role");
    String username = (String) session.getAttribute("username");

    if (userId == null || role == null) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    String dashboardUrl = "patient-dashboard.jsp";
    if ("DOCTOR".equals(role)) {
        dashboardUrl = "doctor-dashboard.jsp";
    } else if ("ADMIN".equals(role)) {
        dashboardUrl = "admin-dashboard.jsp";
    }
    request.setAttribute("dashboardUrl", dashboardUrl);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Change Password - MediCare ODAS</title>
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
                <c:choose>
                    <c:when test="${sessionScope.role == 'PATIENT'}">
                        <a href="patient-dashboard.jsp">My Dashboard</a>
                        <a href="doctors">Find Doctors</a>
                        <a href="appointment.jsp">Book Appointment</a>
                        <a href="profile">My Profile</a>
                    </c:when>
                    <c:when test="${sessionScope.role == 'DOCTOR'}">
                        <a href="doctor-dashboard.jsp">Doctor Portal</a>
                        <a href="doctors">Doctors Directory</a>
                    </c:when>
                    <c:when test="${sessionScope.role == 'ADMIN'}">
                        <a href="admin-dashboard.jsp">Admin Dashboard</a>
                        <a href="doctors">Doctors Directory</a>
                    </c:when>
                </c:choose>
                <a href="change-password.jsp" class="active">Change Password</a>
                <span class="user-badge">🔑 <c:out value="${sessionScope.username}" /> (<c:out value="${sessionScope.role}" />)</span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container">
        <div class="auth-wrapper" style="padding: 30px 0;">
            <div class="auth-card" style="max-width: 480px;">
                <h2>Security Settings</h2>
                <p class="auth-subtitle">Change account credentials and keep your account secure.</p>

                <c:if test="${param.msg == 'updated'}">
                    <div class="alert alert-success">
                        ✅ Your password has been changed successfully!
                    </div>
                </c:if>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger">
                        ⚠️ <c:out value="${errorMessage}" />
                    </div>
                </c:if>

                <form action="change-password" method="post" class="auth-form" autocomplete="off">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <div class="form-group">
                        <label for="currentPassword">Current Password <span style="color: var(--danger-color);">*</span></label>
                        <input type="password" id="currentPassword" name="currentPassword" required 
                               placeholder="Enter existing password">
                    </div>

                    <div class="form-group">
                        <label for="newPassword">New Password <span style="color: var(--danger-color);">*</span></label>
                        <input type="password" id="newPassword" name="newPassword" required minlength="8"
                               placeholder="Minimum 8 characters">
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">Confirm New Password <span style="color: var(--danger-color);">*</span></label>
                        <input type="password" id="confirmPassword" name="confirmPassword" required minlength="8"
                               placeholder="Re-enter new password">
                    </div>

                    <div style="display: flex; gap: 12px; margin-top: 10px;">
                        <button type="submit" class="btn btn-primary btn-block">Update Password</button>
                        <a href="<c:out value='${dashboardUrl}' />" class="btn btn-secondary btn-block" style="text-decoration: none; text-align: center;">Cancel</a>
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


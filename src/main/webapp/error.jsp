<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    if (statusCode == null) statusCode = 404;

    String role = (String) session.getAttribute("role");
    String homeUrl = "index.jsp";
    if ("PATIENT".equals(role)) {
        homeUrl = "patient-dashboard.jsp";
    } else if ("DOCTOR".equals(role)) {
        homeUrl = "doctor-dashboard.jsp";
    } else if ("ADMIN".equals(role)) {
        homeUrl = "admin-dashboard.jsp";
    }
    request.setAttribute("homeUrl", homeUrl);
    request.setAttribute("statusCode", statusCode);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Notice - MediCare ODAS</title>
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
                <a href="<c:out value='${homeUrl}'/>">Return to Home</a>
                <a href="doctors">Find Doctors</a>
            </nav>
        </div>
    </header>

    <main class="page-container">
        <div class="auth-wrapper" style="padding: 60px 20px;">
            <div class="auth-card" style="max-width: 520px; text-align: center;">
                <div style="font-size: 3.5rem; margin-bottom: 12px;">
                    <c:choose>
                        <c:when test="${statusCode == 404}">🔍</c:when>
                        <c:otherwise>🛡️</c:otherwise>
                    </c:choose>
                </div>
                <h2>
                    <c:choose>
                        <c:when test="${statusCode == 404}">Resource Not Found</c:when>
                        <c:otherwise>Service Notice</c:otherwise>
                    </c:choose>
                </h2>
                <p class="auth-subtitle" style="margin-bottom: 24px;">
                    <c:choose>
                        <c:when test="${statusCode == 404}">
                            The page or record you requested could not be found or has been moved.
                        </c:when>
                        <c:otherwise>
                            An unexpected condition occurred while processing your request. Please rest assured your data is safe.
                        </c:otherwise>
                    </c:choose>
                </p>

                <div style="display: flex; justify-content: center; gap: 12px;">
                    <a href="<c:out value='${homeUrl}'/>" class="btn btn-primary">Return to Portal</a>
                    <a href="javascript:history.back()" class="btn btn-secondary">Go Back</a>
                </div>
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


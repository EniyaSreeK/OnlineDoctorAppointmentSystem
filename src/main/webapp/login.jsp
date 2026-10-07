<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Sign In - MediCare ODAS</title>
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
                <a href="index.jsp">Home</a>
                <a href="doctors">Find Doctors</a>
                <a href="signup.jsp">Register</a>
                <a href="login.jsp" class="btn btn-primary btn-nav">Sign In</a>
            </nav>
        </div>
    </header>

    <main class="auth-wrapper">
        <div class="auth-card">
            <h2>Welcome Back</h2>
            <p class="auth-subtitle">Sign in to your patient, doctor, or administrator portal.</p>

            <%-- Success and Error Banners --%>
            <% if ("true".equals(request.getParameter("registered"))) { %>
                <div class="alert alert-success">
                    ✅ Registration successful! Please log in with your credentials.
                </div>
            <% } %>

            <% if ("true".equals(request.getParameter("logout"))) { %>
                <div class="alert alert-info">
                    ℹ️ You have been safely logged out.
                </div>
            <% } %>

            <% if ("unauthorized".equals(request.getParameter("error"))) { %>
                <div class="alert alert-warning">
                    ⚠️ Please log in to access your portal.
                </div>
            <% } %>

            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger">
                    ⚠️ <c:out value="${errorMessage}" />
                </div>
            </c:if>

            <form action="login" method="post" class="auth-form" autocomplete="off">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" 
                           value="<c:out value='${username}' />" 
                           required autofocus placeholder="Enter your username">
                </div>

                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required placeholder="Enter your password">
                </div>

                <button type="submit" class="btn btn-primary btn-block">Sign In</button>
            </form>

            <div class="auth-footer">
                <p>New to MediCare? <a href="signup.jsp">Register as a new patient</a></p>
                <p style="margin-top: 8px; font-size: 0.8rem; color: #94a3b8;">Default Admin: admin / admin123</p>
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


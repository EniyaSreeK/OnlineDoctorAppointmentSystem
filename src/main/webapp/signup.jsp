<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Patient Registration - MediCare ODAS</title>
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
                <a href="signup.jsp" class="active">Register</a>
                <a href="login.jsp" class="btn btn-primary btn-nav">Sign In</a>
            </nav>
        </div>
    </header>

    <main class="auth-wrapper">
        <div class="auth-card">
            <h2>Patient Registration</h2>
            <p class="auth-subtitle">Create a patient account to schedule and manage doctor appointments.</p>

            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger">
                    ⚠️ <c:out value="${errorMessage}" />
                </div>
            </c:if>

            <form action="signup" method="post" class="auth-form" autocomplete="off">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <div class="form-group">
                    <label for="name">Full Name</label>
                    <input type="text" id="name" name="name" 
                           value="<c:out value='${name}' />" 
                           required placeholder="e.g. Eleanor Vance">
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label for="age">Age</label>
                        <input type="number" id="age" name="age" min="1" max="120" step="1" 
                               value="<c:out value='${age}' />" 
                               required placeholder="e.g. 28">
                    </div>

                    <div class="form-group">
                        <label for="gender">Gender</label>
                        <select id="gender" name="gender" required>
                            <option value="">-- Select Gender --</option>
                            <option value="Male" ${fn:toUpperCase(gender) == 'MALE' ? 'selected' : ''}>Male</option>
                            <option value="Female" ${fn:toUpperCase(gender) == 'FEMALE' ? 'selected' : ''}>Female</option>
                            <option value="Other" ${fn:toUpperCase(gender) == 'OTHER' ? 'selected' : ''}>Other</option>
                        </select>
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label for="phone">Phone (10 Digits)</label>
                        <input type="tel" id="phone" name="phone" pattern="[0-9]{10}"
                               value="<c:out value='${phone}' />" 
                               required placeholder="e.g. 9876543210">
                    </div>

                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" 
                               value="<c:out value='${email}' />" 
                               required placeholder="e.g. user@example.com">
                    </div>
                </div>

                <div class="form-group">
                    <label for="address">Residential Address</label>
                    <input type="text" id="address" name="address" 
                           value="<c:out value='${address}' />" 
                           placeholder="House/Street, City">
                </div>

                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" 
                           value="<c:out value='${username}' />" 
                           required placeholder="Choose a unique username">
                </div>

                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" minlength="8" required placeholder="At least 8 characters">
                </div>

                <button type="submit" class="btn btn-primary btn-block">Complete Registration</button>
            </form>

            <div class="auth-footer">
                <p>Already have an account? <a href="login.jsp">Sign in here</a></p>
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


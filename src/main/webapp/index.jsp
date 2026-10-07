<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.odas.Appointment" %>
<%@ page import="com.odas.dao.AppointmentDAO" %>
<%@ page import="com.odas.dao.PatientDAO" %>
<%@ page import="com.odas.Patient" %>
<%@ page import="com.odas.util.TimeSlotUtil" %>
<%
    String userRole = (String) session.getAttribute("role");
    String username = (String) session.getAttribute("username");
    boolean isLoggedIn = (session.getAttribute("userId") != null && userRole != null);

    String dashboardUrl = "patient-dashboard.jsp";
    if ("DOCTOR".equals(userRole)) {
        dashboardUrl = "doctor-dashboard.jsp";
    } else if ("ADMIN".equals(userRole)) {
        dashboardUrl = "admin-dashboard.jsp";
    }
    request.setAttribute("dashboardUrl", dashboardUrl);
    request.setAttribute("isLoggedIn", isLoggedIn);
    request.setAttribute("userRole", userRole);
    request.setAttribute("username", username);

    // STAGE 4: In-app 24-hour reminder check for logged-in patient
    List<Appointment> reminderAppts = new ArrayList<>();
    if (isLoggedIn && "PATIENT".equals(userRole)) {
        try {
            Integer patientId = (Integer) session.getAttribute("patientId");
            if (patientId == null) {
                Integer userId = (Integer) session.getAttribute("userId");
                Patient p = new PatientDAO().getPatientByUserId(userId);
                if (p != null) {
                    patientId = p.getId();
                    session.setAttribute("patientId", patientId);
                    session.setAttribute("patientName", p.getName());
                }
            }
            if (patientId != null) {
                List<Appointment> appts = new AppointmentDAO().getAppointmentsByPatientId(patientId);
                for (Appointment a : appts) {
                    if ("SCHEDULED".equalsIgnoreCase(a.getStatus()) &&
                        TimeSlotUtil.isWithinNext24Hours(a.getAppointmentDate(), a.getAppointmentTime())) {
                        reminderAppts.add(a);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
    request.setAttribute("reminderAppts", reminderAppts);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>MediCare - Online Doctor Appointment System</title>
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
                <a href="index.jsp" class="active">Home</a>
                <a href="doctors">Find Doctors</a>
                <c:choose>
                    <c:when test="${isLoggedIn}">
                        <a href="<c:out value='${dashboardUrl}' />">My Dashboard</a>
                        <c:if test="${userRole == 'PATIENT'}">
                            <a href="appointment.jsp">Book Appointment</a>
                            <a href="profile">My Profile</a>
                        </c:if>
                        <a href="change-password.jsp">Change Password</a>
                        <span class="user-badge"><c:out value="${userRole}" />: <c:out value="${username}" /></span>
                        <a href="logout" class="btn btn-logout">Logout</a>
                    </c:when>
                    <c:otherwise>
                        <a href="signup.jsp">Register</a>
                        <a href="login.jsp" class="btn btn-primary btn-nav">Sign In</a>
                    </c:otherwise>
                </c:choose>
            </nav>
        </div>
    </header>

    <main>
        <c:if test="${not empty reminderAppts}">
            <div style="max-width: 1200px; margin: 20px auto 0 auto; padding: 0 24px;">
                <c:forEach var="rem" items="${reminderAppts}">
                    <div class="alert alert-warning" style="background-color: #fef3c7; border: 1px solid #f59e0b; border-left: 6px solid #d97706; color: #92400e; padding: 14px 18px; border-radius: var(--radius-sm); margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; box-shadow: var(--shadow-sm);">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <span style="font-size: 1.5rem;">🔔</span>
                            <div>
                                <strong style="color: #b45309;">Upcoming Consultation Reminder:</strong> You have an appointment with 
                                <strong><c:out value="${rem.doctorName}" /></strong> (<c:out value="${rem.doctorSpecialization}" />) 
                                scheduled on <strong><c:out value="${rem.appointmentDate.toLocalDateTime().toLocalDate()}" /></strong> 
                                at <strong><c:out value="${rem.formattedTime}" /></strong> (within the next 24 hours).
                            </div>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <a href="appointment.jsp?rescheduleId=<c:out value='${rem.id}' />" class="btn btn-sm btn-secondary" style="background: #ffffff; color: #92400e; border: 1px solid #d97706; white-space: nowrap;">
                                Reschedule
                            </a>
                            <a href="patient-dashboard.jsp" class="btn btn-sm btn-primary" style="white-space: nowrap;">
                                View Dashboard
                            </a>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <%-- Hero Section --%>
        <section class="hero-section">
            <div class="hero-container">
                <div class="hero-content">
                    <span class="hero-badge">🌟 Professional Healthcare at Your Fingertips</span>
                    <h1>Your Health Journey Starts with the Right Doctor</h1>
                    <p class="hero-desc">
                        Connect with certified medical specialists, schedule consultations seamlessly, and manage your appointments with our reliable Oracle-powered healthcare booking portal.
                    </p>
                    <div class="hero-cta-group">
                        <a href="doctors" class="btn btn-primary btn-lg">Explore Doctors</a>
                        <c:choose>
                            <c:when test="${not isLoggedIn}">
                                <a href="signup.jsp" class="btn btn-outline btn-lg">New Patient Sign Up</a>
                            </c:when>
                            <c:otherwise>
                                <a href="appointment.jsp" class="btn btn-outline btn-lg">Book Consultation</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="hero-stats">
                        <div class="stat-item">
                            <span class="stat-number">100%</span>
                            <span class="stat-label">Verified Doctors</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-number">24/7</span>
                            <span class="stat-label">Online Booking</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-number">Instant</span>
                            <span class="stat-label">Slot Confirmation</span>
                        </div>
                    </div>
                </div>
                <div class="hero-card-preview">
                    <div class="preview-card">
                        <div class="preview-header">
                            <div class="preview-avatar">👨‍⚕️</div>
                            <div>
                                <h4>Dr. Kumar</h4>
                                <span class="badge badge-primary">Cardiologist</span>
                            </div>
                        </div>
                        <p class="preview-text">"Providing comprehensive cardiac care and preventive wellness consultations."</p>
                        <div class="preview-footer">
                            <span class="preview-status">🟢 Available for Appointments</span>
                            <a href="doctors" class="btn btn-sm btn-primary">Book Now</a>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <%-- Core Features Section --%>
        <section class="features-section">
            <div class="section-container">
                <div class="section-header">
                    <h2>Why Choose MediCare ODAS?</h2>
                    <p>Designed for patients, healthcare professionals, and hospital administrators.</p>
                </div>

                <div class="features-grid">
                    <div class="feature-card">
                        <div class="feature-icon">🩺</div>
                        <h3>Verified Specialists</h3>
                        <p>Browse qualified physicians across multiple specializations with full transparency on medical profiles.</p>
                    </div>

                    <div class="feature-card">
                        <div class="feature-icon">⚡</div>
                        <h3>Real-Time Slot Verification</h3>
                        <p>Automated database constraints eliminate double-booking and ensure your schedule is confirmed instantly.</p>
                    </div>

                    <div class="feature-card">
                        <div class="feature-icon">🔒</div>
                        <h3>Secure & Confidential</h3>
                        <p>Enterprise Oracle 21c security, cryptographic SHA-256 password protection, and role-based access control.</p>
                    </div>

                    <div class="feature-card">
                        <div class="feature-icon">📊</div>
                        <h3>Dedicated Dashboards</h3>
                        <p>Tailored portals for Patients, Doctors, and Administrators to monitor and complete consultations effortlessly.</p>
                    </div>
                </div>
            </div>
        </section>
    </main>

    <footer>
        <div class="footer-container">
            <div class="footer-brand">
                <span class="logo-icon">🏥</span>
                <strong>MediCare ODAS</strong>
                <p>Enterprise Online Doctor Appointment Scheduling System.</p>
            </div>
            <div class="footer-links">
                <a href="index.jsp">Home</a>
                <a href="doctors">Doctors Directory</a>
                <a href="signup.jsp">Patient Sign Up</a>
                <a href="login.jsp">Portal Login</a>
            </div>
        </div>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


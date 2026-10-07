<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="java.util.List" %>
<%@ page import="com.odas.Doctor" %>
<%@ page import="com.odas.dao.DoctorDAO" %>
<%
    // Prevent caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    // Retrieve search filters from request attributes or query parameters
    String searchName = request.getAttribute("searchName") != null 
            ? (String) request.getAttribute("searchName") 
            : (request.getParameter("name") != null ? request.getParameter("name").trim() : "");

    String searchSpec = request.getAttribute("searchSpecialization") != null 
            ? (String) request.getAttribute("searchSpecialization") 
            : (request.getParameter("specialization") != null ? request.getParameter("specialization").trim() : "");

    String searchLoc = request.getAttribute("searchLocation") != null 
            ? (String) request.getAttribute("searchLocation") 
            : (request.getParameter("location") != null ? request.getParameter("location").trim() : "");

    String searchDay = request.getAttribute("searchAvailableDay") != null 
            ? (String) request.getAttribute("searchAvailableDay") 
            : (request.getParameter("availableDay") != null ? request.getParameter("availableDay").trim() : "");

    boolean hasActiveFilter = !searchName.isEmpty() || !searchSpec.isEmpty() || !searchLoc.isEmpty() || !searchDay.isEmpty();

    // Retrieve doctors from request attribute (set by DoctorServlet) or query DAO directly
    List<Doctor> doctors = (List<Doctor>) request.getAttribute("doctors");
    if (doctors == null) {
        try {
            DoctorDAO doctorDAO = new DoctorDAO();
            if (hasActiveFilter) {
                doctors = doctorDAO.searchDoctors(searchName, searchSpec, searchLoc, searchDay);
            } else {
                doctors = doctorDAO.getAllDoctors();
            }
        } catch (Exception e) {
            doctors = java.util.Collections.emptyList();
        }
    }

    // Check login state for dynamic navigation bar
    String userRole = (String) session.getAttribute("role");
    String username = (String) session.getAttribute("username");
    boolean isLoggedIn = (session.getAttribute("userId") != null && userRole != null);

    String dashboardUrl = "patient-dashboard.jsp";
    if ("DOCTOR".equals(userRole)) {
        dashboardUrl = "doctor-dashboard.jsp";
    } else if ("ADMIN".equals(userRole)) {
        dashboardUrl = "admin-dashboard.jsp";
    }

    request.setAttribute("searchName", searchName);
    request.setAttribute("searchSpec", searchSpec);
    request.setAttribute("searchLoc", searchLoc);
    request.setAttribute("searchDay", searchDay);
    request.setAttribute("hasActiveFilter", hasActiveFilter);
    request.setAttribute("doctors", doctors);
    request.setAttribute("userRole", userRole);
    request.setAttribute("username", username);
    request.setAttribute("isLoggedIn", isLoggedIn);
    request.setAttribute("dashboardUrl", dashboardUrl);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Find Doctors - MediCare ODAS</title>
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
                <a href="index.jsp">Home</a>
                <a href="doctors" class="active">Find Doctors</a>
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

    <main class="page-container">
        <div class="page-header">
            <h2>Our Medical Specialists</h2>
            <p>Search board-certified physicians by name, specialty, location, or available days and book appointments.</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger">
                ⚠️ <c:out value="${errorMessage}" />
            </div>
        </c:if>

        <!-- STAGE 3: Doctor Search & Filter Bar -->
        <div class="doctor-filter-card">
            <h3>🔍 Search & Filter Doctors</h3>
            <form action="doctors" method="get" class="doctor-filter-form">
                <div class="form-group">
                    <label for="name">Doctor Name</label>
                    <input type="text" id="name" name="name" 
                           placeholder="e.g. Kumar, Priya" 
                           value="<c:out value='${searchName}' />">
                </div>

                <div class="form-group">
                    <label for="specialization">Specialization</label>
                    <input type="text" id="specialization" name="specialization" 
                           placeholder="e.g. Cardiologist, Dermatologist" 
                           value="<c:out value='${searchSpec}' />">
                </div>

                <div class="form-group">
                    <label for="location">Hospital / Location</label>
                    <input type="text" id="location" name="location" 
                           placeholder="e.g. Apollo, SkinCare, Chennai" 
                           value="<c:out value='${searchLoc}' />">
                </div>

                <div class="form-group">
                    <label for="availableDay">Available Day</label>
                    <select id="availableDay" name="availableDay">
                        <option value="" ${empty searchDay ? 'selected' : ''}>All Days</option>
                        <option value="Mon" ${searchDay == 'Mon' ? 'selected' : ''}>Monday</option>
                        <option value="Tue" ${searchDay == 'Tue' ? 'selected' : ''}>Tuesday</option>
                        <option value="Wed" ${searchDay == 'Wed' ? 'selected' : ''}>Wednesday</option>
                        <option value="Thu" ${searchDay == 'Thu' ? 'selected' : ''}>Thursday</option>
                        <option value="Fri" ${searchDay == 'Fri' ? 'selected' : ''}>Friday</option>
                        <option value="Sat" ${searchDay == 'Sat' ? 'selected' : ''}>Saturday</option>
                        <option value="Sun" ${searchDay == 'Sun' ? 'selected' : ''}>Sunday</option>
                    </select>
                </div>

                <div class="doctor-filter-actions">
                    <button type="submit" class="btn btn-primary" style="white-space: nowrap;">
                        Filter
                    </button>
                    <c:if test="${hasActiveFilter}">
                        <a href="doctors" class="btn btn-secondary" style="white-space: nowrap;">
                            Reset
                        </a>
                    </c:if>
                </div>
            </form>
        </div>

        <!-- Search Results Count Banner -->
        <div class="search-results-summary">
            <div>
                <c:choose>
                    <c:when test="${hasActiveFilter}">
                        Showing <strong><c:out value="${not empty doctors ? fn:length(doctors) : 0}" /></strong> doctor(s) matching your criteria
                    </c:when>
                    <c:otherwise>
                        Showing all <strong><c:out value="${not empty doctors ? fn:length(doctors) : 0}" /></strong> medical specialists
                    </c:otherwise>
                </c:choose>
            </div>
            <c:if test="${hasActiveFilter}">
                <div>
                    <a href="doctors" style="color: var(--primary-blue); font-weight: 600; text-decoration: none;">&times; Clear all filters</a>
                </div>
            </c:if>
        </div>

        <!-- Doctor Cards Grid -->
        <div class="doctor-container">
            <c:choose>
                <c:when test="${not empty doctors}">
                    <c:forEach var="doc" items="${doctors}">
                        <div class="doctor-card">
                            <div class="doctor-avatar">
                                <span class="avatar-icon">👨‍⚕️</span>
                            </div>
                            <h3><c:out value="${doc.name}" /></h3>
                            <div>
                                <span class="specialization-badge">🩺 <c:out value="${doc.specialization}" /></span>
                            </div>
                            <p class="doctor-id-label">Physician ID: #<c:out value="${doc.id}" /></p>

                            <c:if test="${not empty doc.qualification}">
                                <div class="doctor-info-row">
                                    <span>🎓 <c:out value="${doc.qualification}" /></span>
                                </div>
                            </c:if>

                            <c:if test="${not empty doc.experience && doc.experience > 0}">
                                <div class="doctor-info-row">
                                    <span>⏳ <c:out value="${doc.experience}" /> Years Experience</span>
                                </div>
                            </c:if>

                            <c:if test="${not empty doc.location}">
                                <div class="doctor-info-row">
                                    <span>📍 <c:out value="${doc.location}" /></span>
                                </div>
                            </c:if>

                            <c:if test="${not empty doc.availableDays}">
                                <div class="doctor-info-row">
                                    <span>🗓️ <c:out value="${doc.availableDays}" /></span>
                                </div>
                            </c:if>

                            <c:if test="${not empty doc.availableTime}">
                                <div class="doctor-info-row">
                                    <span>⏰ <c:out value="${doc.availableTime}" /></span>
                                </div>
                            </c:if>

                            <div>
                                <span class="doctor-fee-badge">
                                    Fee: ₹<c:out value="${doc.consultationFee != null ? String.format('%.2f', doc.consultationFee) : '500.00'}" />
                                </span>
                            </div>
                            
                            <a href="appointment.jsp?doctorId=<c:out value='${doc.id}' />" 
                               class="btn btn-primary btn-block">
                                Book Appointment
                            </a>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="empty-state full-width">
                        <div class="empty-icon">🔍</div>
                        <h3>No Doctors Found</h3>
                        <p>
                            <c:choose>
                                <c:when test="${hasActiveFilter}">
                                    No physicians matched your current filter criteria. Try adjusting your keywords or clearing the filter.
                                </c:when>
                                <c:otherwise>
                                    There are currently no doctors listed in the database. Please check back soon.
                                </c:otherwise>
                            </c:choose>
                        </p>
                        <c:if test="${hasActiveFilter}">
                            <div style="margin-top: 16px;">
                                <a href="doctors" class="btn btn-primary">View All Doctors</a>
                            </div>
                        </c:if>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


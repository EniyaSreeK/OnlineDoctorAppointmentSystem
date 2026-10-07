<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.odas.Doctor" %>
<%@ page import="com.odas.Appointment" %>
<%@ page import="com.odas.dao.DoctorDAO" %>
<%@ page import="com.odas.dao.AppointmentDAO" %>
<%@ page import="com.odas.util.TimeSlotUtil" %>
<%
    // Prevent browser caching so dynamic parameter selections are always fresh
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    // Ensure patient session
    Integer patientId = (Integer) session.getAttribute("patientId");
    String patientName = (String) session.getAttribute("patientName");
    String userRole = (String) session.getAttribute("role");

    if (session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    if (patientName == null) {
        patientName = (String) session.getAttribute("username");
    }

    DoctorDAO doctorDAO = new DoctorDAO();
    AppointmentDAO appointmentDAO = new AppointmentDAO();
    List<Doctor> doctorList = doctorDAO.getAllDoctors();

    // Check for Reschedule mode
    String rescheduleIdParam = request.getParameter("rescheduleId");
    boolean isReschedule = false;
    Appointment rescheduleAppt = null;
    int rescheduleId = -1;

    if (rescheduleIdParam != null && !rescheduleIdParam.trim().isEmpty()) {
        try {
            rescheduleId = Integer.parseInt(rescheduleIdParam.trim());
            rescheduleAppt = appointmentDAO.getAppointmentById(rescheduleId);
            if (rescheduleAppt != null && "SCHEDULED".equalsIgnoreCase(rescheduleAppt.getStatus())) {
                isReschedule = true;
            }
        } catch (NumberFormatException ignored) {}
    }

    // Determine selected doctor
    String selectedDoctorIdParam = request.getParameter("doctorId");
    String selectedDoctorNameParam = request.getParameter("doctorName");
    int selectedDoctorId = -1;
    String selectedDoctorName = "";
    String selectedDoctorTime = "09:00-17:00";
    String selectedDoctorDays = "Mon-Sun";

    if (isReschedule && rescheduleAppt != null) {
        selectedDoctorId = rescheduleAppt.getDoctorId();
        selectedDoctorName = rescheduleAppt.getDoctorName();
    } else if (selectedDoctorIdParam != null && !selectedDoctorIdParam.trim().isEmpty()) {
        try {
            selectedDoctorId = Integer.parseInt(selectedDoctorIdParam.trim());
        } catch (NumberFormatException ignored) {}
    }

    // Match doctor object and availability details
    Doctor selectedDoctor = null;
    if (selectedDoctorId > 0) {
        for (Doctor d : doctorList) {
            if (d.getId() == selectedDoctorId) {
                selectedDoctor = d;
                selectedDoctorName = d.getName();
                if (d.getAvailableTime() != null && !d.getAvailableTime().trim().isEmpty()) {
                    selectedDoctorTime = d.getAvailableTime();
                }
                if (d.getAvailableDays() != null && !d.getAvailableDays().trim().isEmpty()) {
                    selectedDoctorDays = d.getAvailableDays();
                }
                break;
            }
        }
    } else if (selectedDoctorNameParam != null && !selectedDoctorNameParam.trim().isEmpty()) {
        for (Doctor d : doctorList) {
            if (d.getName().equalsIgnoreCase(selectedDoctorNameParam.trim())) {
                selectedDoctor = d;
                selectedDoctorId = d.getId();
                selectedDoctorName = d.getName();
                if (d.getAvailableTime() != null && !d.getAvailableTime().trim().isEmpty()) {
                    selectedDoctorTime = d.getAvailableTime();
                }
                if (d.getAvailableDays() != null && !d.getAvailableDays().trim().isEmpty()) {
                    selectedDoctorDays = d.getAvailableDays();
                }
                break;
            }
        }
    }

    // Pre-fill date and time slot
    String prefilledDate = "";
    String prefilledTime = "";
    if (isReschedule && rescheduleAppt != null) {
        prefilledDate = rescheduleAppt.getAppointmentDate().toLocalDateTime().toLocalDate().toString();
        prefilledTime = rescheduleAppt.getAppointmentTime();
    } else {
        if (request.getParameter("date") != null && !request.getParameter("date").trim().isEmpty()) {
            prefilledDate = request.getParameter("date").trim();
        }
        if (request.getParameter("appointmentTime") != null && !request.getParameter("appointmentTime").trim().isEmpty()) {
            prefilledTime = request.getParameter("appointmentTime").trim();
        } else if (request.getParameter("time") != null && !request.getParameter("time").trim().isEmpty()) {
            prefilledTime = request.getParameter("time").trim();
        }
    }

    // Generate initial time slots server-side for selected doctor (30 min intervals)
    List<String> initialSlots = TimeSlotUtil.generateTimeSlots(selectedDoctorTime, 30);

    request.setAttribute("isReschedule", isReschedule);
    request.setAttribute("rescheduleId", rescheduleId);
    request.setAttribute("selectedDoctorId", selectedDoctorId);
    request.setAttribute("selectedDoctorName", selectedDoctorName);
    request.setAttribute("selectedDoctorTime", selectedDoctorTime);
    request.setAttribute("selectedDoctorDays", selectedDoctorDays);
    request.setAttribute("prefilledDate", prefilledDate);
    request.setAttribute("prefilledTime", prefilledTime);
    request.setAttribute("todayDate", java.time.LocalDate.now().toString());
    request.setAttribute("initialSlots", initialSlots);
    request.setAttribute("doctorList", doctorList);
    request.setAttribute("patientName", patientName);
    request.setAttribute("userRole", userRole);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title><c:out value="${isReschedule ? 'Reschedule Consultation' : 'Book Doctor Appointment'}" /> - MediCare ODAS</title>
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
                <a href="doctors">Find Doctors</a>
                <c:choose>
                    <c:when test="${userRole == 'PATIENT'}">
                        <a href="patient-dashboard.jsp">My Dashboard</a>
                        <a href="profile">My Profile</a>
                    </c:when>
                    <c:when test="${userRole == 'DOCTOR'}">
                        <a href="doctor-dashboard.jsp">Doctor Portal</a>
                    </c:when>
                    <c:when test="${userRole == 'ADMIN'}">
                        <a href="admin-dashboard.jsp">Admin Dashboard</a>
                    </c:when>
                </c:choose>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge">👤 <c:out value="${patientName}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container">
        <div class="auth-wrapper" style="padding: 20px 0;">
            <div class="auth-card" style="max-width: 540px;">
                <h2><c:out value="${isReschedule ? 'Reschedule Consultation' : 'Book Doctor Consultation'}" /></h2>
                <p class="auth-subtitle">
                    <c:choose>
                        <c:when test="${isReschedule}">
                            Select a new date and 30-minute time slot for your appointment with <c:out value="${selectedDoctorName}" />.
                        </c:when>
                        <c:otherwise>
                            Select your specialist physician, preferred consultation date, and available 30-minute time slot.
                        </c:otherwise>
                    </c:choose>
                </p>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger">
                        ⚠️ <c:out value="${errorMessage}" />
                    </div>
                </c:if>

                <form action="appointment" method="post" class="auth-form" autocomplete="off" id="bookingForm">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <%-- Hidden input to preserve doctorName and rescheduleId --%>
                    <input type="hidden" name="doctorName" id="doctorNameHidden" value="<c:out value='${selectedDoctorName}' />">
                    <c:if test="${isReschedule}">
                        <input type="hidden" name="rescheduleId" value="<c:out value='${rescheduleId}' />">
                    </c:if>

                    <div class="form-group">
                        <label>Patient Name</label>
                        <input type="text" name="patientName" value="<c:out value='${patientName}' />" readonly 
                               style="background-color: #f8fafc; cursor: not-allowed; color: var(--text-muted);">
                    </div>

                    <div class="form-group">
                        <label for="doctorId">Specialist Doctor</label>
                        <c:choose>
                            <c:when test="${isReschedule}">
                                <input type="text" value="<c:out value='${selectedDoctorName}' />" readonly
                                       style="background-color: #f8fafc; cursor: not-allowed; font-weight: 600; color: var(--text-primary);">
                                <input type="hidden" name="doctorId" id="doctorId" value="<c:out value='${selectedDoctorId}' />">
                            </c:when>
                            <c:otherwise>
                                <select name="doctorId" id="doctorId" required>
                                    <option value="" ${selectedDoctorId <= 0 ? 'selected="selected"' : ''}>-- Choose a Doctor --</option>
                                    <c:forEach var="doc" items="${doctorList}">
                                        <c:set var="docTime" value="${not empty doc.availableTime ? doc.availableTime : '09:00-17:00'}" />
                                        <c:set var="docDays" value="${not empty doc.availableDays ? doc.availableDays : 'Mon-Sun'}" />
                                        <option value="<c:out value='${doc.id}' />" 
                                                data-name="<c:out value='${doc.name}' />"
                                                data-time="<c:out value='${docTime}' />"
                                                data-days="<c:out value='${docDays}' />"
                                                ${doc.id == selectedDoctorId ? 'selected="selected"' : ''}>
                                            <c:out value="${doc.name}" /> (<c:out value="${doc.specialization}" />)
                                        </option>
                                    </c:forEach>
                                </select>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <%-- Doctor Availability Notice --%>
                    <div id="doctorScheduleNotice" style="background: var(--teal-light); border: 1px solid rgba(13, 148, 136, 0.2); border-radius: var(--radius-sm); padding: 10px 14px; margin-bottom: 16px; font-size: 0.88rem; color: var(--teal-hover);">
                        🗓️ <strong>Availability:</strong> <span id="scheduleDaysText"><c:out value="${selectedDoctorDays}" /></span> &bull; ⏰ <span id="scheduleTimeText"><c:out value="${selectedDoctorTime}" /></span>
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                        <div class="form-group">
                            <label for="date">Appointment Date</label>
                            <input type="date" id="date" name="date" required 
                                   min="<c:out value='${todayDate}' />"
                                   value="<c:out value='${prefilledDate}' />">
                        </div>

                        <div class="form-group">
                            <label for="appointmentTime">Consultation Slot</label>
                            <select name="appointmentTime" id="appointmentTime" required>
                                <c:forEach var="slot" items="${initialSlots}">
                                    <option value="<c:out value='${slot}' />" ${slot == prefilledTime ? 'selected="selected"' : ''}>
                                        <c:out value="${TimeSlotUtil.formatSlotLabel(slot)}" />
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary btn-block">
                        <c:out value="${isReschedule ? 'Confirm & Reschedule Consultation' : 'Confirm & Book Appointment'}" />
                    </button>
                    <a href="patient-dashboard.jsp" class="btn btn-secondary btn-block" style="margin-top: 10px; text-decoration: none;">
                        Cancel
                    </a>
                </form>
            </div>
        </div>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

    <%-- Dynamic time slots generator & doctor selection synchronizer --%>
    <script>
        (function() {
            var doctorTimeMap = {
                <c:forEach var="d" items="${doctorList}" varStatus="status">
                    "${d.id}": { 
                        "time": "${not empty d.availableTime ? fn:escapeXml(d.availableTime) : '09:00-17:00'}", 
                        "days": "${not empty d.availableDays ? fn:escapeXml(d.availableDays) : 'Mon-Sun'}" 
                    }<c:if test="${not status.last}">,</c:if>
                </c:forEach>
            };

            function generateSlotsForRange(rangeStr) {
                var slots = [];
                var parts = (rangeStr || "09:00-17:00").split("-");
                if (parts.length !== 2) return ["10:00"];

                function parseMinutes(tStr) {
                    var p = tStr.trim().split(":");
                    return parseInt(p[0], 10) * 60 + parseInt(p[1], 10);
                }

                function formatTime(min) {
                    var h = Math.floor(min / 60);
                    var m = min % 60;
                    var hh = (h < 10 ? "0" : "") + h;
                    var mm = (m < 10 ? "0" : "") + m;
                    return hh + ":" + mm;
                }

                function formatLabel(time24) {
                    var p = time24.split(":");
                    var h = parseInt(p[0], 10);
                    var m = p[1];
                    var ampm = h >= 12 ? "PM" : "AM";
                    var h12 = h % 12;
                    if (h12 === 0) h12 = 12;
                    var hh = (h12 < 10 ? "0" : "") + h12;
                    return hh + ":" + m + " " + ampm;
                }

                var startMin = parseMinutes(parts[0]);
                var endMin = parseMinutes(parts[1]);

                for (var cur = startMin; cur + 30 <= endMin; cur += 30) {
                    var t24 = formatTime(cur);
                    slots.push({ value: t24, label: formatLabel(t24) });
                }

                if (slots.length === 0) {
                    var def = formatTime(startMin);
                    slots.push({ value: def, label: formatLabel(def) });
                }
                return slots;
            }

            function updateTimeSlots(docId, selectedSlot) {
                var slotSelect = document.getElementById('appointmentTime');
                if (!slotSelect) return;

                var info = doctorTimeMap[String(docId)] || { time: "09:00-17:00", days: "Mon-Sun" };
                var scheduleDays = document.getElementById('scheduleDaysText');
                var scheduleTime = document.getElementById('scheduleTimeText');
                if (scheduleDays) scheduleDays.textContent = info.days;
                if (scheduleTime) scheduleTime.textContent = info.time;

                var slots = generateSlotsForRange(info.time);
                slotSelect.innerHTML = "";

                for (var i = 0; i < slots.length; i++) {
                    var opt = document.createElement("option");
                    opt.value = slots[i].value;
                    opt.textContent = slots[i].label;
                    if (selectedSlot && slots[i].value === selectedSlot) {
                        opt.selected = true;
                    }
                    slotSelect.appendChild(opt);
                }
            }

            function syncDoctorSelection() {
                var urlParams = new URLSearchParams(window.location.search);
                var reqDocId = urlParams.get('doctorId') || "${selectedDoctorId > 0 ? selectedDoctorId : ''}";
                var sel = document.getElementById('doctorId');
                var nameHidden = document.getElementById('doctorNameHidden');
                if (!sel || sel.tagName !== "SELECT") return;

                if (reqDocId && reqDocId !== "-1" && reqDocId !== "0") {
                    for (var i = 0; i < sel.options.length; i++) {
                        if (String(sel.options[i].value) === String(reqDocId)) {
                            sel.selectedIndex = i;
                            if (nameHidden) {
                                nameHidden.value = sel.options[i].getAttribute('data-name') || "";
                            }
                            updateTimeSlots(reqDocId, "<c:out value='${prefilledTime}' />");
                            return;
                        }
                    }
                }
            }

            var sel = document.getElementById('doctorId');
            if (sel && sel.tagName === "SELECT") {
                sel.addEventListener('change', function() {
                    var selectedOpt = sel.options[sel.selectedIndex];
                    var nameHidden = document.getElementById('doctorNameHidden');
                    if (selectedOpt && nameHidden) {
                        nameHidden.value = selectedOpt.getAttribute('data-name') || "";
                    }
                    if (sel.value) {
                        updateTimeSlots(sel.value, null);
                    }
                });
            }

            syncDoctorSelection();
            document.addEventListener('DOMContentLoaded', syncDoctorSelection);
            window.addEventListener('load', syncDoctorSelection);
            window.addEventListener('pageshow', syncDoctorSelection);
        })();
    </script>

</body>
</html>


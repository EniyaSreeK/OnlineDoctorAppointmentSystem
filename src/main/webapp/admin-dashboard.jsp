<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    // Verify controller invocation; forward to AdminDashboardServlet if directly accessed
    if (request.getAttribute("doctors") == null) {
        request.getRequestDispatcher("/admin-dashboard").forward(request, response);
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Administration Portal - MediCare ODAS</title>
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
                <a href="admin-dashboard.jsp" class="active">Admin Dashboard</a>
                <a href="#reports-section">Reports</a>
                <a href="doctors">Doctors Directory</a>
                <a href="change-password.jsp">Change Password</a>
                <span class="user-badge">🛡️ Admin: <c:out value="${username}" /></span>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="dashboard-container">
        <div class="dashboard-header">
            <h2>Hospital Administration Panel</h2>
            <p>Clinical Resource Management, Physician Provisioning & Global Appointment Oversight</p>
        </div>

        <%-- Alerts --%>
        <c:set var="alertMsg" value="${not empty msg ? msg : param.msg}" />
        <c:set var="alertErr" value="${not empty err ? err : (not empty param.err ? param.err : param.error)}" />
        <c:choose>
            <c:when test="${alertMsg == 'added'}">
                <div class="alert alert-success">✅ New doctor profile created successfully!</div>
            </c:when>
            <c:when test="${alertMsg == 'updated'}">
                <div class="alert alert-success">✅ Doctor profile updated successfully!</div>
            </c:when>
            <c:when test="${alertMsg == 'deleted'}">
                <div class="alert alert-info">ℹ️ Doctor record deleted successfully.</div>
            </c:when>
            <c:when test="${alertMsg == 'cancelled'}">
                <div class="alert alert-info">ℹ️ Appointment was cancelled by administrator.</div>
            </c:when>
            <c:when test="${alertMsg == 'completed'}">
                <div class="alert alert-success">✅ Appointment was marked as completed.</div>
            </c:when>
            <c:when test="${alertMsg == 'patient_deleted'}">
                <div class="alert alert-info">ℹ️ Patient record deleted successfully.</div>
            </c:when>
            <c:when test="${alertMsg == 'patient_updated'}">
                <div class="alert alert-success">✅ Patient demographic and contact details updated successfully!</div>
            </c:when>
            <c:when test="${alertErr == 'username_exists'}">
                <div class="alert alert-danger">⚠️ That doctor username is already registered. Please choose another.</div>
            </c:when>
            <c:when test="${alertErr == 'doctor_has_records'}">
                <div class="alert alert-danger">⚠️ Cannot delete doctor because related clinical records (appointments, prescriptions, or payments) exist. Please resolve related records first.</div>
            </c:when>
            <c:when test="${alertErr == 'delete_failed'}">
                <div class="alert alert-danger">⚠️ Could not delete doctor. A database or dependency constraint prevented deletion.</div>
            </c:when>
            <c:when test="${alertErr == 'add_failed'}">
                <div class="alert alert-danger">⚠️ Could not add doctor. Please verify the profile details and try again.</div>
            </c:when>
            <c:when test="${alertErr == 'update_failed'}">
                <div class="alert alert-danger">⚠️ Could not update doctor profile. Please try again.</div>
            </c:when>
            <c:when test="${alertErr == 'not_found'}">
                <div class="alert alert-danger">⚠️ Requested record was not found.</div>
            </c:when>
            <c:when test="${alertErr == 'missing_fields'}">
                <div class="alert alert-danger">⚠️ Please fill in all required fields.</div>
            </c:when>
            <c:when test="${alertErr == 'patient_has_records'}">
                <div class="alert alert-danger">⚠️ Cannot delete patient because related clinical records (appointments, prescriptions, or payments) exist. Please resolve related records first.</div>
            </c:when>
            <c:when test="${alertErr == 'patient_delete_failed'}">
                <div class="alert alert-danger">⚠️ Could not delete patient. A database constraint prevented deletion.</div>
            </c:when>
            <c:when test="${alertErr == 'patient_not_found'}">
                <div class="alert alert-danger">⚠️ Patient record not found.</div>
            </c:when>
            <c:when test="${alertErr == 'unauthorized'}">
                <div class="alert alert-danger">⚠️ Unauthorized action.</div>
            </c:when>
            <c:when test="${not empty alertErr}">
                <div class="alert alert-danger">⚠️ <c:out value="${alertErr}" /></div>
            </c:when>
        </c:choose>

        <%-- Metric Cards --%>
        <div class="metrics-grid">
            <div class="metric-card">
                <h4>Registered Doctors</h4>
                <div class="metric-value"><c:out value="${fn:length(doctors)}" /></div>
            </div>
            <div class="metric-card">
                <h4>Total Consultations</h4>
                <div class="metric-value" style="color: var(--teal-hover);"><c:out value="${fn:length(allAppointments)}" /></div>
            </div>
            <div class="metric-card">
                <h4>Registered Patients</h4>
                <div class="metric-value" style="color: var(--text-primary);"><c:out value="${fn:length(allPatients)}" /></div>
            </div>
            <div class="metric-card">
                <h4>Total Revenue</h4>
                <div class="metric-value" style="color: #059669;">₹<c:out value="${systemTotalRevenueFormatted}" /></div>
            </div>
        </div>

        <%-- ==========================================================================
             STAGE 7: HOSPITAL ANALYTICS & REPORTS
             Tabular views for Consultations, Payments, Patients & Doctors
             ========================================================================== --%>
        <section id="reports-section" class="dashboard-card" style="margin-bottom: 30px;">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 16px;">
                <div>
                    <h3 style="margin-bottom: 4px;">📊 Hospital Management & Analytical Reports</h3>
                    <p class="text-muted" style="margin: 0;">Comprehensive audit trails, consultation activity, revenue breakdown, and clinician workload analytics.</p>
                </div>
            </div>

            <%-- Report Navigation Tabs --%>
            <div class="report-tabs-nav">
                <button type="button" id="btn-tab-appointments" class="report-tab-btn ${reportTab == 'appointments' ? 'active' : ''}" onclick="switchReportTab('appointments')">
                    📅 Consultations Report (<c:out value="${fn:length(filteredAppts)}" />)
                </button>
                <button type="button" id="btn-tab-payments" class="report-tab-btn ${reportTab == 'payments' ? 'active' : ''}" onclick="switchReportTab('payments')">
                    💳 Payments & Revenue (<c:out value="${fn:length(filteredPayments)}" />)
                </button>
                <button type="button" id="btn-tab-patients" class="report-tab-btn ${reportTab == 'patients' ? 'active' : ''}" onclick="switchReportTab('patients')">
                    👥 Registered Patients (<c:out value="${fn:length(allPatients)}" />)
                </button>
                <button type="button" id="btn-tab-doctors" class="report-tab-btn ${reportTab == 'doctors' ? 'active' : ''}" onclick="switchReportTab('doctors')">
                    👨‍⚕️ Doctors & Workload (<c:out value="${fn:length(doctors)}" />)
                </button>
            </div>

            <%-- TAB 1: Appointments Report --%>
            <div id="report-panel-appointments" class="report-panel" style="display: ${reportTab == 'appointments' ? 'block' : 'none'};">
                <div class="report-filter-bar">
                    <form action="admin-dashboard.jsp" method="get" class="report-filter-form">
                        <input type="hidden" name="reportTab" value="appointments">
                        <div class="report-filter-group">
                            <label for="apptFrom">From Date:</label>
                            <input type="date" id="apptFrom" name="apptFrom" value="<c:out value='${apptFromStr}' />">
                        </div>
                        <div class="report-filter-group">
                            <label for="apptTo">To Date:</label>
                            <input type="date" id="apptTo" name="apptTo" value="<c:out value='${apptToStr}' />">
                        </div>
                        <div class="report-filter-group">
                            <label for="apptStatus">Consultation Status:</label>
                            <select id="apptStatus" name="apptStatus">
                                <option value="ALL" ${apptStatus == 'ALL' ? 'selected' : ''}>All Statuses</option>
                                <option value="SCHEDULED" ${apptStatus == 'SCHEDULED' ? 'selected' : ''}>Scheduled</option>
                                <option value="COMPLETED" ${apptStatus == 'COMPLETED' ? 'selected' : ''}>Completed</option>
                                <option value="CANCELLED" ${apptStatus == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                            </select>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <button type="submit" class="btn btn-primary btn-sm">Filter Consultations</button>
                            <a href="admin-dashboard.jsp?reportTab=appointments#reports-section" class="btn btn-secondary btn-sm">Reset</a>
                        </div>
                    </form>
                </div>

                <%-- Status Breakdown Summary Pills --%>
                <div class="summary-pills">
                    <div class="summary-pill">
                        <span class="summary-pill-label">Total Matched:</span>
                        <span class="summary-pill-value"><c:out value="${fn:length(filteredAppts)}" /></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">Scheduled:</span>
                        <span class="summary-pill-value" style="color: var(--primary-blue);"><c:out value="${countScheduled}" /></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">Completed:</span>
                        <span class="summary-pill-value" style="color: #059669;"><c:out value="${countCompleted}" /></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">Cancelled:</span>
                        <span class="summary-pill-value" style="color: #ef4444;"><c:out value="${countCancelled}" /></span>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${not empty filteredAppts}">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Ref #</th>
                                        <th>Specialist Doctor</th>
                                        <th>Patient Name</th>
                                        <th>Consultation Date & Slot</th>
                                        <th>Appointment Status</th>
                                        <th>Fee Payment</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="a" items="${filteredAppts}">
                                        <c:set var="bClass" value="badge-primary" />
                                        <c:if test="${a.status == 'COMPLETED'}">
                                            <c:set var="bClass" value="badge-success" />
                                        </c:if>
                                        <c:if test="${a.status == 'CANCELLED'}">
                                            <c:set var="bClass" value="badge-danger" />
                                        </c:if>
                                        <c:set var="isPaid" value="${fn:toUpperCase(a.paymentStatus) == 'PAID'}" />
                                        <tr>
                                            <td><strong>#<c:out value="${a.id}" /></strong></td>
                                            <td>
                                                <strong><c:out value="${a.doctorName}" /></strong><br>
                                                <span class="specialization-badge">🩺 <c:out value="${a.doctorSpecialization}" /></span>
                                            </td>
                                            <td>👤 <strong><c:out value="${a.patientName}" /></strong></td>
                                            <td>
                                                📅 <c:out value="${a.appointmentDate != null ? a.appointmentDate.toLocalDateTime().toLocalDate() : ''}" /><br>
                                                <small class="text-muted">⏰ <c:out value="${a.formattedTime}" /></small>
                                            </td>
                                            <td><span class="badge ${bClass}"><c:out value="${a.status}" /></span></td>
                                            <td>
                                                <span class="badge ${isPaid ? 'badge-success' : 'badge-secondary'}">
                                                    <c:out value="${isPaid ? 'PAID' : 'UNPAID'}" />
                                                </span>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${a.status == 'SCHEDULED'}">
                                                        <form action="appointment-action" method="post" style="display:inline;" onsubmit="return confirm('Cancel this patient consultation as administrator?');">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                            <input type="hidden" name="action" value="cancel">
                                                            <input type="hidden" name="id" value="<c:out value='${a.id}' />">
                                                            <button type="submit" class="btn btn-danger btn-sm">Cancel</button>
                                                        </form>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-muted"><c:out value="${a.status}" /></span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state">
                            <div class="empty-icon">📅</div>
                            <p>No consultations found matching the specified date range and status filters.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- TAB 2: Payments & Revenue Report --%>
            <div id="report-panel-payments" class="report-panel" style="display: ${reportTab == 'payments' ? 'block' : 'none'};">
                <div class="report-filter-bar">
                    <form action="admin-dashboard.jsp" method="get" class="report-filter-form">
                        <input type="hidden" name="reportTab" value="payments">
                        <div class="report-filter-group">
                            <label for="payFrom">From Date:</label>
                            <input type="date" id="payFrom" name="payFrom" value="<c:out value='${payFromStr}' />">
                        </div>
                        <div class="report-filter-group">
                            <label for="payTo">To Date:</label>
                            <input type="date" id="payTo" name="payTo" value="<c:out value='${payToStr}' />">
                        </div>
                        <div class="report-filter-group">
                            <label for="payMode">Payment Mode:</label>
                            <select id="payMode" name="payMode">
                                <option value="ALL" ${payMode == 'ALL' ? 'selected' : ''}>All Payment Modes</option>
                                <option value="Card" ${payMode == 'Card' ? 'selected' : ''}>Card (Credit / Debit)</option>
                                <option value="UPI" ${payMode == 'UPI' ? 'selected' : ''}>UPI Transfer</option>
                                <option value="Net Banking" ${payMode == 'Net Banking' ? 'selected' : ''}>Net Banking</option>
                            </select>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <button type="submit" class="btn btn-primary btn-sm">Filter Payments</button>
                            <a href="admin-dashboard.jsp?reportTab=payments#reports-section" class="btn btn-secondary btn-sm">Reset</a>
                        </div>
                    </form>
                </div>

                <%-- Revenue & Mode Breakdown Summary Pills --%>
                <div class="summary-pills">
                    <div class="summary-pill">
                        <span class="summary-pill-label">Total Revenue:</span>
                        <span class="summary-pill-value" style="color: #059669;">₹<c:out value="${filteredTotalRevenueFormatted}" /></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">Transactions:</span>
                        <span class="summary-pill-value"><c:out value="${fn:length(filteredPayments)}" /></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">💳 Card:</span>
                        <span class="summary-pill-value">₹<c:out value="${cardRevenueFormatted}" /> <small class="text-muted" style="font-size:0.8rem;">(<c:out value="${cardCount}" />)</small></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">📱 UPI:</span>
                        <span class="summary-pill-value">₹<c:out value="${upiRevenueFormatted}" /> <small class="text-muted" style="font-size:0.8rem;">(<c:out value="${upiCount}" />)</small></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">🏛️ Net Banking:</span>
                        <span class="summary-pill-value">₹<c:out value="${netBankingRevenueFormatted}" /> <small class="text-muted" style="font-size:0.8rem;">(<c:out value="${netBankingCount}" />)</small></span>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${not empty filteredPayments}">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Txn Ref #</th>
                                        <th>Appt #</th>
                                        <th>Patient</th>
                                        <th>Doctor</th>
                                        <th>Amount (₹)</th>
                                        <th>Payment Mode</th>
                                        <th>Payment Date & Time</th>
                                        <th>Status</th>
                                        <th>Receipt</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="p" items="${filteredPayments}">
                                        <tr>
                                            <td><strong>#TXN-<c:out value="${p.id}" /></strong></td>
                                            <td>#<c:out value="${p.appointmentId}" /></td>
                                            <td>👤 <c:out value="${p.patientName}" /></td>
                                            <td><c:out value="${p.doctorName}" /></td>
                                            <td><strong>₹<c:out value="${String.format('%.2f', p.amount)}" /></strong></td>
                                            <td><span class="badge badge-info"><c:out value="${p.paymentMode}" /></span></td>
                                            <td><small class="text-muted"><c:out value="${p.paymentDate != null ? p.paymentDate.toString().substring(0, 19) : '-'}" /></small></td>
                                            <td><span class="badge badge-success"><c:out value="${p.paymentStatus}" /></span></td>
                                            <td>
                                                <a href="payment?action=receipt&id=<c:out value='${p.id}' />" class="btn btn-secondary btn-sm" target="_blank">
                                                    🧾 Receipt
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state">
                            <div class="empty-icon">💳</div>
                            <p>No payment records found matching the specified date range and payment mode filters.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- TAB 3: Registered Patients Report --%>
            <div id="report-panel-patients" class="report-panel" style="display: ${reportTab == 'patients' ? 'block' : 'none'};">
                <div class="summary-pills">
                    <div class="summary-pill">
                        <span class="summary-pill-label">Total Registered Patients:</span>
                        <span class="summary-pill-value"><c:out value="${fn:length(allPatients)}" /></span>
                    </div>
                    <div class="summary-pill">
                        <span class="summary-pill-label">Matching Filter:</span>
                        <span class="summary-pill-value"><c:out value="${fn:length(filteredPatients)}" /></span>
                    </div>
                </div>

                <%-- Date Range Filter Bar for Registered Patients Report --%>
                <form action="admin-dashboard.jsp" method="get" class="filter-bar" style="margin-bottom: 20px;">
                    <input type="hidden" name="tab" value="patients">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="patientFrom" style="font-size: 0.85rem;">Registered From:</label>
                        <input type="date" id="patientFrom" name="patientFrom" value="<c:out value='${patientFromStr}' />">
                    </div>
                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="patientTo" style="font-size: 0.85rem;">Registered To:</label>
                        <input type="date" id="patientTo" name="patientTo" value="<c:out value='${patientToStr}' />">
                    </div>
                    <button type="submit" class="btn btn-primary" style="height: 42px;">Filter Report</button>
                    <a href="admin-dashboard.jsp?tab=patients" class="btn btn-secondary" style="height: 42px; display: inline-flex; align-items: center;">Reset</a>
                </form>

                <%-- Administrative Edit Patient Form --%>
                <c:if test="${not empty editPatient}">
                    <div style="background-color: #f8fafc; border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 18px; margin-bottom: 24px;">
                        <h4 style="margin: 0 0 12px 0;">✏️ Edit Patient Demographic Details (#PAT-<c:out value="${editPatient.id}" />)</h4>
                        <form action="admin-patients" method="post" style="max-width: 720px;">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="edit">
                            <input type="hidden" name="id" value="<c:out value='${editPatient.id}' />">

                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                                <div class="form-group">
                                    <label for="editPatientName">Patient Full Name <span style="color: var(--danger-color);">*</span></label>
                                    <input type="text" id="editPatientName" name="name" required value="<c:out value='${editPatient.name}' />">
                                </div>
                                <div class="form-group">
                                    <label for="editPatientAge">Age (Years: 0-130) <span style="color: var(--danger-color);">*</span></label>
                                    <input type="number" id="editPatientAge" name="age" min="0" max="130" required value="<c:out value='${editPatient.age}' />">
                                </div>
                            </div>

                            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
                                <div class="form-group">
                                    <label for="editPatientGender">Gender</label>
                                    <select id="editPatientGender" name="gender">
                                        <option value="" ${empty editPatient.gender ? 'selected' : ''}>-- Select --</option>
                                        <option value="Male" ${editPatient.gender == 'Male' ? 'selected' : ''}>Male</option>
                                        <option value="Female" ${editPatient.gender == 'Female' ? 'selected' : ''}>Female</option>
                                        <option value="Other" ${editPatient.gender == 'Other' ? 'selected' : ''}>Other</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label for="editPatientPhone">Phone (10 Digits)</label>
                                    <input type="tel" id="editPatientPhone" name="phone" pattern="[0-9]{10}" value="<c:out value='${editPatient.phone}' />">
                                </div>
                                <div class="form-group">
                                    <label for="editPatientEmail">Email Address</label>
                                    <input type="email" id="editPatientEmail" name="email" value="<c:out value='${editPatient.email}' />">
                                </div>
                            </div>

                            <div class="form-group">
                                <label for="editPatientAddress">Residential Address</label>
                                <input type="text" id="editPatientAddress" name="address" value="<c:out value='${editPatient.address}' />">
                            </div>

                            <div style="display: flex; gap: 12px;">
                                <button type="submit" class="btn btn-primary">Save Patient Changes</button>
                                <a href="admin-dashboard.jsp?tab=patients" class="btn btn-secondary">Cancel</a>
                            </div>
                        </form>
                    </div>
                </c:if>

                <c:choose>
                    <c:when test="${not empty filteredPatients}">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Patient ID</th>
                                        <th>Full Name</th>
                                        <th>Age & Gender</th>
                                        <th>Phone Number</th>
                                        <th>Email Address</th>
                                        <th>Address</th>
                                        <th>Registered Date</th>
                                        <th>Total Appointments</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="pt" items="${filteredPatients}">
                                        <c:set var="apptCount" value="${patientApptCounts[pt.id] != null ? patientApptCounts[pt.id] : 0}" />
                                        <tr>
                                            <td><strong>#PAT-<c:out value="${pt.id}" /></strong></td>
                                            <td>👤 <strong><c:out value="${pt.name}" /></strong></td>
                                            <td><c:out value="${pt.age}" /> yrs / <c:out value="${not empty pt.gender ? pt.gender : '-'}" /></td>
                                            <td><c:out value="${not empty pt.phone ? pt.phone : '-'}" /></td>
                                            <td><c:out value="${not empty pt.email ? pt.email : '-'}" /></td>
                                            <td><c:out value="${not empty pt.address ? pt.address : '-'}" /></td>
                                            <td><c:out value="${not empty pt.formattedCreatedDate ? pt.formattedCreatedDate : '-'}" /></td>
                                            <td>
                                                <span class="badge ${apptCount > 0 ? 'badge-primary' : 'badge-secondary'}">
                                                    <c:out value="${not empty apptCount ? apptCount : 0}" /> booking<c:out value="${apptCount == 1 ? '' : 's'}" />
                                                </span>
                                            </td>
                                            <td>
                                                <div style="display: flex; gap: 8px;">
                                                    <a href="admin-dashboard.jsp?tab=patients&editPatientId=<c:out value='${pt.id}' />" class="btn btn-secondary btn-sm">Edit</a>
                                                    <form action="admin-patients" method="post" style="display:inline;" onsubmit="return confirm('Are you sure you want to delete patient ${fn:escapeXml(pt.name)}?');">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                        <input type="hidden" name="action" value="delete">
                                                        <input type="hidden" name="id" value="<c:out value='${pt.id}' />">
                                                        <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                                                    </form>
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
                            <div class="empty-icon">👥</div>
                            <p>No registered patients found matching the specified date range filter.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- TAB 4: Registered Doctors & Workload Report --%>
            <div id="report-panel-doctors" class="report-panel" style="display: ${reportTab == 'doctors' ? 'block' : 'none'};">
                <div class="report-filter-bar">
                    <form action="admin-dashboard.jsp" method="get" class="report-filter-form">
                        <input type="hidden" name="reportTab" value="doctors">
                        <div class="report-filter-group">
                            <label for="docFrom">From Date:</label>
                            <input type="date" id="docFrom" name="docFrom" value="<c:out value='${docFromStr}' />">
                        </div>
                        <div class="report-filter-group">
                            <label for="docTo">To Date:</label>
                            <input type="date" id="docTo" name="docTo" value="<c:out value='${docToStr}' />">
                        </div>
                        <div class="report-filter-group">
                            <label for="docSpec">Specialization:</label>
                            <input type="text" id="docSpec" name="docSpec" placeholder="e.g. Cardiologist" value="<c:out value='${docSpec}' />">
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <button type="submit" class="btn btn-primary btn-sm">Filter Doctors</button>
                            <a href="admin-dashboard.jsp?reportTab=doctors#reports-section" class="btn btn-secondary btn-sm">Reset</a>
                        </div>
                    </form>
                </div>

                <div class="summary-pills">
                    <div class="summary-pill">
                        <span class="summary-pill-label">Registered Medical Specialists:</span>
                        <span class="summary-pill-value"><c:out value="${fn:length(filteredDoctors)}" /></span>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${not empty filteredDoctors}">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Doctor ID</th>
                                        <th>Physician & Specialization</th>
                                        <th>Qualifications & Experience</th>
                                        <th>Location / Clinic Wing</th>
                                        <th>Weekly Schedule & Hours</th>
                                        <th>Registered Date</th>
                                        <th>Fee (₹)</th>
                                        <th>Workload (Consultations)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="doc" items="${filteredDoctors}">
                                        <c:set var="workload" value="${doctorApptCounts[doc.id] != null ? doctorApptCounts[doc.id] : 0}" />
                                        <tr>
                                            <td><strong>#DOC-<c:out value="${doc.id}" /></strong></td>
                                            <td>
                                                <strong>👨‍⚕️ <c:out value="${doc.name}" /></strong><br>
                                                <span class="specialization-badge">🩺 <c:out value="${doc.specialization}" /></span>
                                            </td>
                                            <td>
                                                <c:out value="${not empty doc.qualification ? doc.qualification : '-'}" /><br>
                                                <small class="text-muted"><c:out value="${doc.experience != null ? doc.experience : ''}" /><c:if test="${doc.experience != null}"> yrs exp</c:if></small>
                                            </td>
                                            <td>📍 <c:out value="${not empty doc.location ? doc.location : '-'}" /></td>
                                            <td>
                                                📅 <c:out value="${not empty doc.availableDays ? doc.availableDays : '-'}" /><br>
                                                <small class="text-muted">⏰ <c:out value="${not empty doc.availableTime ? doc.availableTime : '-'}" /></small>
                                            </td>
                                            <td><c:out value="${not empty doc.formattedCreatedDate ? doc.formattedCreatedDate : '-'}" /></td>
                                            <td><strong>₹<c:out value="${doc.consultationFee != null ? String.format('%.2f', doc.consultationFee) : '500.00'}" /></strong></td>
                                            <td>
                                                <span class="badge ${workload > 0 ? 'badge-success' : 'badge-secondary'}">
                                                    <c:out value="${workload}" /> consultation<c:out value="${workload == 1 ? '' : 's'}" />
                                                </span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state">
                            <div class="empty-icon">👨‍⚕️</div>
                            <p>No registered doctors found matching the specified date range and specialization filter.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <script>
        function switchReportTab(tabName) {
            document.querySelectorAll('.report-panel').forEach(function(el) {
                el.style.display = 'none';
            });
            document.querySelectorAll('.report-tab-btn').forEach(function(btn) {
                btn.classList.remove('active');
            });
            var target = document.getElementById('report-panel-' + tabName);
            if (target) {
                target.style.display = 'block';
            }
            var activeBtn = document.getElementById('btn-tab-' + tabName);
            if (activeBtn) {
                activeBtn.classList.add('active');
            }
        }
        </script>

        <%-- Add / Edit Doctor Form Card --%>
        <section class="dashboard-card" style="margin-bottom: 30px;">
            <h3>
                <c:choose>
                    <c:when test="${not empty editDoctor}">
                        ✏️ Edit Doctor Profile (ID #<c:out value="${editDoctor.id}" />)
                    </c:when>
                    <c:otherwise>
                        ➕ Add New Medical Doctor
                    </c:otherwise>
                </c:choose>
            </h3>
            <p class="text-muted">Register physician credentials, consultation fees, and schedule availability.</p>

            <form action="admin-doctors" method="post" style="max-width: 720px; margin-top: 16px;">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="${not empty editDoctor ? 'edit' : 'add'}">
                <c:if test="${not empty editDoctor}">
                    <input type="hidden" name="id" value="<c:out value='${editDoctor.id}' />">
                </c:if>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label for="name">Doctor Full Name <span style="color: var(--danger-color);">*</span></label>
                        <input type="text" id="name" name="name" required placeholder="e.g. Dr. Jane Wilson"
                               value="<c:out value='${editDoctor.name}' />">
                    </div>

                    <div class="form-group">
                        <label for="specialization">Medical Specialization <span style="color: var(--danger-color);">*</span></label>
                        <input type="text" id="specialization" name="specialization" required placeholder="e.g. Neurologist, Cardiologist"
                               value="<c:out value='${editDoctor.specialization}' />">
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label for="qualification">Qualifications</label>
                        <input type="text" id="qualification" name="qualification" placeholder="e.g. MBBS, MD, FRCS"
                               value="<c:out value='${editDoctor.qualification}' />">
                    </div>

                    <div class="form-group">
                        <label for="experience">Clinical Experience (Years: 0-60)</label>
                        <input type="number" id="experience" name="experience" min="0" max="60" step="1" placeholder="e.g. 10"
                               value="<c:out value='${editDoctor.experience}' />">
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label for="phone">Phone Number (10 Digits)</label>
                        <input type="tel" id="phone" name="phone" pattern="[0-9]{10}" placeholder="e.g. 9876543210"
                               value="<c:out value='${editDoctor.phone}' />">
                    </div>

                    <div class="form-group">
                        <label for="email">Email Address</label>
                        <input type="email" id="email" name="email" placeholder="e.g. dr.wilson@medicare.com"
                               value="<c:out value='${editDoctor.email}' />">
                    </div>
                </div>

                <div class="form-group">
                    <label for="location">Hospital Location / Clinic Wing</label>
                    <input type="text" id="location" name="location" placeholder="e.g. Apollo Block B, Floor 2"
                           value="<c:out value='${editDoctor.location}' />">
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label for="availableDays">Available Days (Mon-Sun)</label>
                        <input type="text" id="availableDays" name="availableDays" placeholder="e.g. Mon,Wed,Fri"
                               value="${not empty editDoctor ? fn:escapeXml(editDoctor.availableDays) : 'Mon,Wed,Fri'}">
                    </div>

                    <div class="form-group">
                        <label for="availableTime">Available Time (HH:mm-HH:mm)</label>
                        <input type="text" id="availableTime" name="availableTime" placeholder="e.g. 09:00-13:00"
                               value="${not empty editDoctor ? fn:escapeXml(editDoctor.availableTime) : '09:00-13:00'}">
                    </div>

                    <div class="form-group">
                        <label for="consultationFee">Fee (₹, &gt; 0) <span style="color: var(--danger-color);">*</span></label>
                        <input type="number" id="consultationFee" name="consultationFee" min="0.01" max="999999.99" step="0.01" required placeholder="500.00"
                               value="${not empty editDoctor && editDoctor.consultationFee != null ? editDoctor.consultationFee : '500.00'}">
                    </div>
                </div>

                <c:if test="${empty editDoctor}">
                    <div style="background-color: #f8fafc; padding: 18px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; font-size: 0.95rem; color: var(--text-primary);">Doctor Portal Access (Optional):</h4>
                        <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 12px;">Create credentials if the doctor will log in to their consultation dashboard.</p>
                        <div class="form-group" style="margin-bottom: 12px;">
                            <label for="username">Doctor Portal Username</label>
                            <input type="text" id="username" name="username" placeholder="e.g. dr_wilson">
                        </div>
                        <div class="form-group" style="margin-bottom: 0;">
                            <label for="password">Doctor Portal Password</label>
                            <input type="password" id="password" name="password" placeholder="Password for login">
                        </div>
                    </div>
                </c:if>

                <div style="display: flex; gap: 12px;">
                    <button type="submit" class="btn btn-primary">
                        <c:out value="${not empty editDoctor ? 'Save Changes' : 'Create Doctor Profile'}" />
                    </button>
                    <c:if test="${not empty editDoctor}">
                        <a href="admin-dashboard.jsp" class="btn btn-secondary">Cancel Edit</a>
                    </c:if>
                </div>
            </form>
        </section>

        <%-- Doctor Directory Management Table --%>
        <section class="dashboard-card" style="margin-bottom: 30px;">
            <h3>Physician Directory Management (<c:out value="${fn:length(doctors)}" />)</h3>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Ref #</th>
                            <th>Doctor & Specialization</th>
                            <th>Qualifications & Exp</th>
                            <th>Schedule & Hours</th>
                            <th>Fee (₹)</th>
                            <th>Portal User</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="d" items="${doctors}">
                            <tr>
                                <td><strong>#<c:out value="${d.id}" /></strong></td>
                                <td>
                                    <strong>👨‍⚕️ <c:out value="${d.name}" /></strong><br>
                                    <span class="specialization-badge">🩺 <c:out value="${d.specialization}" /></span>
                                </td>
                                <td>
                                    <c:out value="${not empty d.qualification ? d.qualification : '-'}" /><br>
                                    <small class="text-muted"><c:out value="${d.experience != null ? d.experience : ''}" /><c:if test="${d.experience != null}"> yrs exp</c:if></small>
                                </td>
                                <td>
                                    📅 <c:out value="${not empty d.availableDays ? d.availableDays : '-'}" /><br>
                                    ⏰ <c:out value="${not empty d.availableTime ? d.availableTime : '-'}" />
                                </td>
                                <td><strong>₹<c:out value="${d.consultationFee != null ? String.format('%.2f', d.consultationFee) : '500.00'}" /></strong></td>
                                <td><c:out value="${d.userId != null ? 'User #' : 'None'}" /><c:if test="${d.userId != null}"><c:out value="${d.userId}" /></c:if></td>
                                <td>
                                    <a href="admin-dashboard.jsp?editId=<c:out value='${d.id}' />" class="btn btn-secondary btn-sm">Edit</a>
                                    <form action="admin-doctors" method="post" style="display:inline;" onsubmit="return confirm('Are you sure you want to delete ${fn:escapeXml(d.name)}? All appointments scheduled with this doctor will also be removed.');">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="<c:out value='${d.id}' />">
                                        <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


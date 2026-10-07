<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ page import="com.odas.Prescription" %>
<%@ page import="com.odas.Appointment" %>
<%@ page import="com.odas.Doctor" %>
<%@ page import="com.odas.Patient" %>
<%@ page import="com.odas.dao.DoctorDAO" %>
<%@ page import="com.odas.dao.PatientDAO" %>
<%
    // Prevent browser caching
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String role = (String) session.getAttribute("role");
    if (session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp?error=unauthorized");
        return;
    }

    Prescription p = (Prescription) request.getAttribute("prescription");
    if (p == null) {
        response.sendRedirect("index.jsp");
        return;
    }

    Appointment appt = (Appointment) request.getAttribute("appointment");

    DoctorDAO doctorDAO = new DoctorDAO();
    Doctor doc = doctorDAO.getDoctorById(p.getDoctorId());

    PatientDAO patientDAO = new PatientDAO();
    Patient patient = patientDAO.getPatientById(p.getPatientId());

    String returnDashboard = "patient-dashboard.jsp";
    if ("DOCTOR".equals(role)) {
        returnDashboard = "doctor-dashboard.jsp";
    } else if ("ADMIN".equals(role)) {
        returnDashboard = "admin-dashboard.jsp";
    }

    String displayPatientName = (patient != null && patient.getName() != null) ? patient.getName() : p.getPatientName();
    String displayDoctorName = (doc != null && doc.getName() != null) ? doc.getName() : p.getDoctorName();
    String displayDoctorSpecialty = (doc != null && doc.getSpecialization() != null) ? doc.getSpecialization() : p.getDoctorSpecialization();
    Object prescriptionDate = p.getCreatedDate() != null ? p.getCreatedDate().toLocalDateTime().toLocalDate() : java.time.LocalDate.now();

    request.setAttribute("p", p);
    request.setAttribute("doc", doc);
    request.setAttribute("patient", patient);
    request.setAttribute("returnDashboard", returnDashboard);
    request.setAttribute("displayPatientName", displayPatientName);
    request.setAttribute("displayDoctorName", displayDoctorName);
    request.setAttribute("displayDoctorSpecialty", displayDoctorSpecialty);
    request.setAttribute("prescriptionDate", prescriptionDate);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Prescription #<c:out value="${p.id}" /> - MediCare ODAS</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        .prescription-container {
            max-width: 760px;
            margin: 32px auto;
            background: #ffffff;
            border-radius: var(--radius-md);
            border: 1px solid var(--border-color);
            box-shadow: var(--shadow-md);
            padding: 40px;
        }
        .clinic-letterhead {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding-bottom: 24px;
            border-bottom: 2px solid var(--primary-blue);
            margin-bottom: 24px;
        }
        .clinic-brand {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .clinic-brand .icon {
            font-size: 2.4rem;
        }
        .clinic-brand h1 {
            font-size: 1.5rem;
            color: var(--text-primary);
            margin: 0;
            line-height: 1.2;
        }
        .clinic-brand p {
            font-size: 0.85rem;
            color: var(--text-muted);
            margin: 2px 0 0 0;
        }
        .prescription-meta {
            text-align: right;
            font-size: 0.85rem;
            color: var(--text-muted);
        }
        .prescription-meta strong {
            color: var(--text-primary);
            font-size: 0.95rem;
        }
        .parties-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 24px;
            background-color: #f8fafc;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-sm);
            padding: 18px 20px;
            margin-bottom: 28px;
        }
        .party-box h4 {
            font-size: 0.8rem;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--text-muted);
            margin-bottom: 6px;
        }
        .party-box .name {
            font-size: 1.1rem;
            font-weight: 700;
            color: var(--text-primary);
        }
        .party-box .subtext {
            font-size: 0.86rem;
            color: var(--text-secondary);
            margin-top: 2px;
        }
        .diagnosis-card {
            background-color: var(--teal-light);
            border-left: 4px solid var(--teal-accent);
            padding: 14px 18px;
            border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
            margin-bottom: 24px;
        }
        .diagnosis-card h4 {
            font-size: 0.85rem;
            text-transform: uppercase;
            color: var(--teal-hover);
            margin-bottom: 4px;
            font-weight: 700;
        }
        .diagnosis-card .diag-text {
            font-size: 1.1rem;
            font-weight: 700;
            color: var(--text-primary);
        }
        .rx-section {
            margin-bottom: 36px;
        }
        .rx-header {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 1.3rem;
            font-weight: 800;
            color: var(--primary-blue);
            margin-bottom: 14px;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 8px;
        }
        .rx-body {
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-sm);
            padding: 20px;
            font-family: inherit;
            font-size: 0.98rem;
            line-height: 1.7;
            color: var(--text-primary);
            white-space: pre-wrap;
        }
        .signature-footer {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            padding-top: 30px;
            border-top: 1px dashed var(--border-color);
            margin-top: 30px;
        }
        .doc-signature {
            text-align: right;
        }
        .sig-line {
            width: 200px;
            border-top: 1px solid var(--text-primary);
            margin-bottom: 6px;
            margin-left: auto;
        }
        .print-actions {
            display: flex;
            justify-content: center;
            gap: 16px;
            margin: 24px 0 40px 0;
        }
        @media print {
            header, footer, .print-actions, .alert {
                display: none !important;
            }
            body {
                background: #ffffff !important;
            }
            .prescription-container {
                border: none !important;
                box-shadow: none !important;
                padding: 0 !important;
                margin: 0 !important;
                max-width: 100% !important;
            }
        }
    </style>
</head>
<body>

    <header>
        <div class="header-container">
            <a href="index.jsp" class="logo">
                <span class="logo-icon">🏥</span>
                <span class="logo-text">MediCare <span>ODAS</span></span>
            </a>
            <nav>
                <a href="<c:out value='${returnDashboard}' />">My Dashboard</a>
                <a href="doctors">Find Doctors</a>
                <c:if test="${sessionScope.role == 'PATIENT'}">
                    <a href="appointment.jsp">Book Appointment</a>
                    <a href="profile">My Profile</a>
                </c:if>
                <a href="change-password.jsp">Change Password</a>
                <a href="logout" class="btn btn-logout">Logout</a>
            </nav>
        </div>
    </header>

    <main class="page-container" style="padding-top: 10px;">
        <c:if test="${param.msg == 'created'}">
            <div class="alert alert-success" style="max-width: 760px; margin: 16px auto 0 auto;">
                ✅ Clinical prescription successfully generated and saved to patient record!
            </div>
        </c:if>
        <c:if test="${param.msg == 'updated'}">
            <div class="alert alert-success" style="max-width: 760px; margin: 16px auto 0 auto;">
                ✅ Clinical prescription successfully updated!
            </div>
        </c:if>

        <div class="prescription-container">
            <%-- Letterhead --%>
            <div class="clinic-letterhead">
                <div class="clinic-brand">
                    <span class="icon">🏥</span>
                    <div>
                        <h1>MediCare Health &amp; Specialty Clinic</h1>
                        <p>Department of Clinical Consultations &bull; Oracle ODAS</p>
                    </div>
                </div>
                <div class="prescription-meta">
                    <div>Prescription: <strong>#RX-<c:out value="${p.id}" /></strong></div>
                    <div>Consultation Ref: <strong>#<c:out value="${p.appointmentId}" /></strong></div>
                    <div>Date: <strong><c:out value="${prescriptionDate}" /></strong></div>
                </div>
            </div>

            <%-- Patient & Physician Details --%>
            <div class="parties-grid">
                <div class="party-box">
                    <h4>PATIENT INFORMATION</h4>
                    <div class="name">👤 <c:out value="${displayPatientName}" /></div>
                    <div class="subtext">
                        <c:if test="${not empty patient && patient.age > 0}">
                            Age: <c:out value="${patient.age}" /> yrs &bull; Gender: <c:out value="${not empty patient.gender ? patient.gender : 'N/A'}" /><br>
                        </c:if>
                        <c:if test="${not empty patient && not empty patient.phone}">
                            Contact: <c:out value="${patient.phone}" />
                        </c:if>
                    </div>
                </div>
                <div class="party-box">
                    <h4>ATTENDING PHYSICIAN</h4>
                    <div class="name">👨‍⚕️ <c:out value="${displayDoctorName}" /></div>
                    <div class="subtext">
                        Specialty: <strong><c:out value="${displayDoctorSpecialty}" /></strong><br>
                        <c:if test="${not empty doc && not empty doc.qualification}">
                            <c:out value="${doc.qualification}" /><br>
                        </c:if>
                        <c:if test="${not empty doc && not empty doc.location}">
                            📍 <c:out value="${doc.location}" />
                        </c:if>
                    </div>
                </div>
            </div>

            <%-- Clinical Diagnosis --%>
            <div class="diagnosis-card">
                <h4>CLINICAL DIAGNOSIS</h4>
                <div class="diag-text"><c:out value="${p.diagnosis}" /></div>
            </div>

            <%-- Rx - Prescription Details --%>
            <div class="rx-section">
                <div class="rx-header">
                    <span>℞</span>
                    <span>Medications &amp; Dosage Instructions</span>
                </div>
                <div class="rx-body"><c:out value="${p.prescriptionDetails}" /></div>
            </div>

            <%-- Official Signature Footer --%>
            <div class="signature-footer">
                <div>
                    <p style="font-size: 0.8rem; color: var(--text-muted); margin: 0;">
                        Verified by MediCare Online Doctor Appointment System<br>
                        System Generated Clinical Document
                    </p>
                </div>
                <div class="doc-signature">
                    <div class="sig-line"></div>
                    <strong style="color: var(--text-primary); font-size: 0.95rem;">
                        <c:out value="${displayDoctorName}" />
                    </strong>
                    <div style="font-size: 0.82rem; color: var(--text-muted);">
                        <c:out value="${displayDoctorSpecialty}" />
                    </div>
                </div>
            </div>
        </div>

        <%-- Actions --%>
        <div class="print-actions">
            <button onclick="window.print();" class="btn btn-primary" style="display: flex; align-items: center; gap: 8px;">
                <span>🖨️</span>
                <span>Print / Download Prescription</span>
            </button>
            <c:if test="${sessionScope.role == 'DOCTOR' || sessionScope.role == 'ADMIN'}">
                <a href="prescription?action=edit&id=<c:out value='${p.id}' />" class="btn btn-secondary" style="display: flex; align-items: center; gap: 8px; text-decoration: none;">
                    <span>✏️</span>
                    <span>Edit Prescription</span>
                </a>
            </c:if>
            <a href="<c:out value='${returnDashboard}' />" class="btn btn-secondary">
                Return to Dashboard
            </a>
        </div>
    </main>

    <footer>
        <div class="footer-bottom">
            <p>&copy; 2026 Online Doctor Appointment System (ODAS). Built on Oracle 21c XE & Jakarta EE.</p>
        </div>
    </footer>

</body>
</html>


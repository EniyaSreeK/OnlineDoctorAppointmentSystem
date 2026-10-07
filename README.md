# ODAS — Online Doctor Appointment System

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?logo=openjdk&logoColor=white)](#)
[![Jakarta EE 10](https://img.shields.io/badge/Jakarta%20EE-10-orange?logo=eclipse-ide&logoColor=white)](#)
[![Tomcat 10.1](https://img.shields.io/badge/Apache%20Tomcat-10.1-F8DC75?logo=apachetomcat&logoColor=black)](#)
[![Oracle 21c XE](https://img.shields.io/badge/Oracle%20DB-21c%20XE-F80000?logo=oracle&logoColor=white)](#)
[![JUnit 4](https://img.shields.io/badge/JUnit-121%20Tests%20Passing-brightgreen?logo=junit5&logoColor=white)](#)
[![Security](https://img.shields.io/badge/Security-PBKDF2%20%7C%20CSRF%20%7C%20RBAC-blue)](#)

A multi-tiered web application for hospitals and clinics that lets patients find doctors, book and manage appointments, pay consultation fees online, and download prescriptions, while doctors and administrators manage schedules, consultations, records, and reports.

Built with **Java 21, Jakarta EE 10 (Servlet 6.0 / JSP 3.0), Apache Tomcat 10.1, Oracle Database 21c XE (`xepdb1`)** and a responsive HTML5/CSS3 front end. No ORM and no heavyweight framework: plain JDBC with `PreparedStatement` throughout.

---

## 🏛️ System Architecture

ODAS follows a clean MVC pipeline with a dedicated security filter chain and a DAO layer:

```
[Browser Client (JSP 3.0 / JSTL / HTML5 / Responsive CSS3)]
                         │
                         ▼
[CsrfFilter] ──────────── (Valid session-scoped CSRF token on state-changing requests? Yes ──> Next / No ──> 403 Forbidden)
                         │
                         ▼
[AuthFilter] ──────────── (Active session? Role allowed for requested URL? Yes ──> Next / No ──> Redirect to login/dashboard)
                         │
                         ▼
[Controllers (Servlets)] ─ (Login, Signup, Logout, Profile, ChangePassword, Appointment, AppointmentAction,
                            Doctor, AdminDoctor, AdminPatient, AdminDashboard, PatientDashboard, Payment, Prescription)
                         │                              │
                         ▼                              ▼
[Data Access Objects] ──────────     [Background Services]
 UserDAO, DoctorDAO, PatientDAO,      ReminderScheduler ──> NotificationService (Email / SMS)
 AppointmentDAO, PaymentDAO,
 PrescriptionDAO
                         │
                         ▼
[JDBC Connection] ──────── (DBConnection via ojdbc11)
                         │
                         ▼
[Oracle Database 21c XE (USERS, DOCTORS, PATIENTS, APPOINTMENTS, PAYMENTS, PRESCRIPTIONS)]
```

---

## 🚀 Module Completion Status

### ✅ Module 0: Foundation & Infrastructure
- **Database connectivity:** Central `DBConnection` over `ojdbc11`, configured through git-ignored local properties or `ODAS_DB_*` environment variables. No credentials in source control.
- **Schema & DDL:** Self-contained `schema.sql` with primary keys, foreign keys, check constraints, and partial unique indexes used by booking.
- **Project baseline:** Maven build (JDK 21, Jakarta EE 10), Tomcat 10.1 deployment, UTF-8 enforced across JSPs and build configuration.
- **Verification:** `DatabaseVerificationTest` confirms schema reachability; DB tests skip gracefully when Oracle is unavailable.

### ✅ Module 1: Authentication, Registration & Role-Based Access Control
- **Login (FR1):** Username and password authentication with role-based redirect to Administrator, Doctor, or Patient dashboards.
- **Registration:** Patient self-signup with username, email, and phone uniqueness checks; `USERS` and `PATIENTS` rows created in one atomic transaction.
- **Password security:** PBKDF2WithHmacSHA256 (210,000 iterations, 16-byte random salt) with transparent upgrade of legacy SHA-256 hashes.
- **Filters:** `CsrfFilter` (session-scoped tokens, constant-time compare) and `AuthFilter` (session guard and per-role URL authorization).
- **Logout (FR13):** Full session invalidation and redirect to login page.

### ✅ Module 2: Profile & Password Management
- **Profile (FR2):** Patients view and update their own details with server-side validation (10-digit phone, email format, age 1–120).
- **Change Password (FR12):** Current / new / confirm flow with complexity check, mismatch rejection, and PBKDF2 re-hash.

### ✅ Module 3: Doctor Management & Search
- **Admin CRUD (FR3):** Add, edit, and delete doctors, with specialization, location, available days (`Mon,Wed,Fri`), hours (`HH:mm-HH:mm`), and fee (> 0, ≤ 999,999.99).
- **Doctor self-service:** Doctors update their own consultation days and hours.
- **Search (FR4):** Case-insensitive search by name, specialization, location, and available days.

### ✅ Module 4: Appointment Booking, Slots & Rescheduling
- **Booking (FR5):** Doctor, date, and slot selection with instant confirmation.
- **Dynamic slots:** 30-minute intervals generated strictly within doctor's available days and hours.
- **Conflict prevention:** Doctor and patient double-booking blocked at application layer and by `UQ_ACTIVE_APPT_SLOT` and `UQ_ACTIVE_PATIENT_SLOT`; cancelled slots become bookable again.
- **Time validation:** Past dates and past times on the current day are rejected server-side.
- **Cancel & reschedule (FR6):** Patients and admins can cancel; patients reschedule in place; attending doctor can decline or cancel with patient notification.
- **Lifecycle:** `SCHEDULED → COMPLETED` or `SCHEDULED → CANCELLED`; both end states are terminal.
- **History (FR8):** Upcoming versus past and cancelled appointments in patient dashboard.

### ✅ Module 5: Prescription Management
- **Doctor workflow (FR10):** Add and edit digital prescriptions for completed consultations; only attending doctor may write or edit.
- **Patient access:** View and print or download prescriptions.
- **Medical history:** Doctors view patient's past consultations, diagnoses, and prescriptions.

### ✅ Module 6: Online Payment & Receipts
- **Server-side fees (FR9):** Fee is read from doctor record, never from client.
- **Modes & status:** Card, UPI, and Net Banking; `PENDING → PAID`, updated atomically in database transaction.
- **Ownership checks:** Only the booking patient can pay for an appointment.
- **Receipt:** Printable payment receipt after successful checkout (`payment-receipt.jsp`).

### ✅ Module 7: Admin Dashboard, Patient Management & Reports
- **Patient management:** View, edit (with duplicate checks and CSRF protection), and delete patients, with per-patient booking count.
- **Operational reports (FR11):** Appointments by date range and status; Payments by date range and mode, with revenue totals for Card, UPI, and Net Banking.
- **Registration reports (FR11):** Registered Patients and Registered Doctors by date range, with specialization filtering for doctors.

### ✅ Module 8: Reminders & Notifications
- **Scheduler (FR7):** `ReminderScheduler` runs every 15 minutes, finds appointments in next 24 hours, dispatches reminders, and sets `REMINDER_SENT = 1`.
- **Channels:** `NotificationService` sends Email (Jakarta/Angus Mail over SMTP) and SMS (mock provider by default, Twilio optional).
- **Event notifications:** Cancellations, including doctor declines, notify the affected patient.

### ✅ Module 9: Sample Data, Documentation & CI
- **Seed data:** Idempotent `db/seed_data.sql` (`MERGE INTO`) with 30 doctors, 18 departments, 6 cities, 73 patients, 155 appointments, 125 payments, and 80 prescriptions.
- **Documentation:** Full README and `CHANGELOG.md`.
- **CI:** GitHub Actions workflow building and testing on JDK 21 for every push and pull request.

---

## 👥 User Roles

| Role | Capabilities |
| :--- | :--- |
| **Administrator** | Manage doctors (add, edit, delete, schedule, fees) and patients (view, edit, delete); view hospital-wide analytics; generate reports for patients, doctors, appointments, and payments filtered by date range. |
| **Doctor** | Manage own consultation days and hours; view assigned appointments; mark consultations completed; decline or cancel appointments; add and edit prescriptions; view patient medical history. |
| **Patient** | Register and manage profile; search doctors; book, reschedule, and cancel appointments; pay fees online and print receipts; view and download prescriptions; view appointment history. |

---

## 📋 Features by SRS Requirement

| ID | Feature | Description |
| :--- | :--- | :--- |
| **FR1** | User Login | Username and password authentication with role-based redirect to correct dashboard. |
| **FR2** | Patient Management | Self-registration, profile updates, and admin-side view, edit, and delete of patient records. |
| **FR3** | Doctor Management | Admin adds, edits, and deletes doctors and configures availability, location, and fees. |
| **FR4** | Doctor Search | Search by name, specialization, location, and available days (case-insensitive). |
| **FR5** | Appointment Booking | Pick doctor, date, and 30-minute slot; instant confirmation. |
| **FR6** | Cancel / Reschedule | Patients and admins can cancel; patients reschedule in place; attending doctors can decline or cancel with patient notification. |
| **FR7** | Appointment Reminders | Background scheduler sends email/SMS reminders for upcoming appointments within 24 hours. |
| **FR8** | Appointment History | Upcoming vs. past and cancelled appointments in patient dashboard. |
| **FR9** | Online Payment | Server-side fee lookup, atomic payment status update, and printable receipt. |
| **FR10** | Prescriptions | Doctors create and edit digital prescriptions; patients view and download them; doctors see patient history. |
| **FR11** | Reports | Admin reports for Registered Patients, Registered Doctors, Appointments, and Payments filtered by date range. |
| **FR12** | Change Password | Current / new / confirm flow with complexity check and PBKDF2 re-hashing. |
| **FR13** | Logout | Full session invalidation and redirect to login page. |

---

## 🔐 Security Highlights

- **Password Hashing:** PBKDF2WithHmacSHA256 (210,000 iterations, 16-byte cryptographically secure random salt). Legacy SHA-256 hashes are upgraded transparently on next login. Passwords are never stored or returned in plain text.
- **CSRF Protection:** Session-scoped tokens generated with `SecureRandom`, validated by `CsrfFilter` using constant-time string comparison (`MessageDigest.isEqual`).
- **Role-Based Access Control:** `AuthFilter` guards `/admin-*`, `/doctor-*`, `/patient-*`, `/appointment*`, `/payment*`, `/prescription*`, `/profile`, and `/change-password`. Cross-role URL tampering redirects to caller's own dashboard.
- **SQL Injection Prevention:** 100% parameterized `PreparedStatement` queries across all DAOs.
- **XSS Prevention:** All user-supplied output rendered through `<c:out>` or `fn:escapeXml`.
- **IDOR Protection:** Patients can only reschedule, cancel, or pay for their own appointments; doctors can only write prescriptions for their own completed consultations.
- **No Secrets in Git:** Credentials load from git-ignored local properties or environment variables.

---

## 📅 Appointment & Booking Rules

- Slots are generated in **30-minute** intervals strictly inside doctor's available days and hours.
- **No doctor double-booking:** Enforced in application layer and by Oracle unique index `UQ_ACTIVE_APPT_SLOT`.
- **No patient double-booking:** Enforced in application layer and by unique index `UQ_ACTIVE_PATIENT_SLOT`.
- Both indexes cover only active appointments, so a cancelled slot can be booked again immediately.
- Past dates and past times on the current day are rejected server-side.
- Rescheduling updates the same appointment record (no duplicate or orphaned rows).
- Status model: `SCHEDULED → COMPLETED` or `SCHEDULED → CANCELLED`. Both end states are terminal.
- Cancelling by doctor, patient, or admin frees the slot and notifies affected parties.

### Input Validation Rules

| Field | Rule |
| :--- | :--- |
| **Phone** | Exactly 10 digits |
| **Email** | RFC 5322 style format check |
| **Patient Age** | 1 – 120 |
| **Available Days** | Format like `Mon,Wed,Fri` |
| **Consultation Hours** | Format `HH:mm-HH:mm` |
| **Consultation Fee** | Greater than 0 and at most 999,999.99 |
| **Uniqueness** | Username, email, and phone must be unique |

---

## 💳 Payments & Prescriptions

- **Payment Fee:** Read **server-side** from doctor record; client cannot tamper with the amount.
- **Payment Modes:** Card, UPI, Net Banking. Status: `PENDING` or `PAID`, updated atomically with `conn.setAutoCommit(false)`.
- **Receipts:** Printable payment receipt generated upon successful checkout (`payment-receipt.jsp`).
- **Prescriptions:** Only attending doctor can create or edit; patients and doctors can view and print or download.

---

## 🔔 Reminders & Notifications

- `ReminderScheduler` runs every **15 minutes**, queries appointments in next **24 hours** where `REMINDER_SENT = 0`, dispatches reminders, and sets `REMINDER_SENT = 1`.
- `NotificationService` supports **Email** (Jakarta/Angus Mail over SMTP with STARTTLS) and **SMS** (mock provider by default, Twilio optional).
- Messages omit medical diagnoses and clinical notes to protect healthcare privacy.

---

## 🗄️ Database Schema

Defined in `src/main/resources/schema.sql`:

| Table | Purpose | Key Constraints |
| :--- | :--- | :--- |
| `USERS` | Login accounts, hashed passwords, role (`ADMIN`, `DOCTOR`, `PATIENT`) | Primary key, unique username, check role |
| `DOCTORS` | Doctor profile, specialization, location, availability, fee | Foreign key `USER_ID`, non-null contact |
| `PATIENTS` | Patient demographics and contact details | Foreign key `USER_ID`, check age |
| `APPOINTMENTS` | Bookings with status, payment state, reminder flag | Foreign keys doctor/patient, partial unique slot indexes |
| `PAYMENTS` | Payment records, mode, status, receipt timestamp | Foreign key appointment/patient, check status |
| `PRESCRIPTIONS` | Diagnosis and medication per completed appointment | Unique foreign key `APPOINTMENT_ID` |

---

## 🌱 Sample Dataset

The idempotent script [`db/seed_data.sql`](file:///c:/Users/HP/OneDrive/Desktop/SOFTWARE%20DEVELOPMENT/OnlineDoctorAppointmentSystem/onlinedoctorappointmentsystem/db/seed_data.sql) (`MERGE INTO`, safe to re-run) loads a realistic dataset:

- **30 doctors** across 18 clinical departments and 6 cities
- **73 registered patients**
- **155 consultations** (Scheduled, Completed, Cancelled; past and future)
- **125 payments** and **80 prescriptions**

All seeded accounts use secure PBKDF2 password hashes.

---

## 📋 Demo Credentials

| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Hospital Administrator (analytics, doctors, patients, reports) |
| **Doctor** | `dr_kumar` | `doctor123` | Dr. Rajesh Kumar — Cardiologist (consultations, prescriptions) |
| **Doctor** | `dr_priya` | `doctor123` | Dr. Priya Nair — Dermatologist |
| **Patient** | `rahul1` | `password123` | Rahul Verma (booking, payments, prescriptions) |
| **Patient** | `anita_m` | `password123` | Anita Menon |

*(Additional pre-seeded patient accounts also use password `password123`. New patients can self-register at `/signup.jsp`.)*

---

## 🛠️ Setup & Running Instructions

### Prerequisites
- **JDK 21**
- **Apache Tomcat 10.1.x**
- **Oracle Database 21c XE** on `localhost:1521`, pluggable database `xepdb1`
- **Apache Maven 3.9+** (or included Maven wrapper)

### 1. Database Setup
Connect to Oracle XE and run the schema and seed scripts as `ODAS_USER`:

```sql
sqlplus odas_user/YOUR_PASSWORD@localhost:1521/xepdb1 @"src/main/resources/schema.sql"
sqlplus odas_user/YOUR_PASSWORD@localhost:1521/xepdb1 @"db/seed_data.sql"
```

### 2. Configuration
Copy the configuration template (git-ignored, never committed):

```bash
cp src/main/resources/db.properties.example src/main/resources/db.local.properties
```

Edit `src/main/resources/db.local.properties`:
```properties
db.url=jdbc:oracle:thin:@localhost:1521/xepdb1
db.username=ODAS_USER
db.password=YOUR_ORACLE_PASSWORD
```

*Or set environment variables:* `ODAS_DB_URL`, `ODAS_DB_USER`, `ODAS_DB_PASSWORD`.  
*For Email/SMS settings, see `config.example`.*

### 3. Build & Deploy
```bash
# Package into WAR
mvn clean package -DskipTests
```

Copy `target/onlinedoctorappointmentsystem.war` into Tomcat's `webapps/` directory and start Tomcat:

```
http://localhost:8080/onlinedoctorappointmentsystem/login.jsp
```

### 4. Run Automated Tests
```bash
mvn test
```

---

## 🧪 Automated Testing

**121 JUnit 4 tests across 19 test classes.**

| Module | Test Classes | Scope Covered | Oracle Required? |
| :--- | :--- | :--- | :--- |
| **0 — Foundation** | `DatabaseVerificationTest`, `Stage1SchemaExtensionTest`, `ModelEntitiesTest` | Connectivity, schema extensions, model entities | Partly |
| **1 — Auth & RBAC** | `AuthValidationTest`, `PasswordUtilTest`, `CsrfFilterTest` | Login/signup validation, PBKDF2 hashing, CSRF tokens | No |
| **2 — Profile & Password** | `Stage2ProfileAndPasswordTest`, `ValidationUtilTest` | Profile updates, password changes, input validation rules | No |
| **3 — Doctors & Search** | `DoctorListingTest`, `Stage3DoctorSearchTest` | Doctor listing and search filters | Yes |
| **4 — Appointments** | `Stage1_5SlotBookingTest`, `Stage4TimeSlotAndRescheduleTest`, `TimeSlotUtilTest`, `AppointmentFlowTest`, `AppointmentStatusTransitionTest` | Slot generation, conflicts, rescheduling, status transitions | Mixed |
| **5 — Prescriptions** | `Stage5PrescriptionManagementTest` | Create, edit, view, authorization checks | Yes |
| **6 — Payments** | `Stage6PaymentManagementTest` | Fee lookup, atomic payment status, receipts | Yes |
| **7 — Admin & Reports** | `Stage7AdminReportsTest` | Date-range reports and aggregates | Yes |
| **8 — Notifications** | `NotificationServiceTest` | Email and SMS dispatch (mock provider) | No |

*Database integration tests gracefully skip via `Assume.assumeTrue(DBConnection.isAvailable())` when Oracle is unreachable (e.g. in CI pipelines), while all pure unit tests execute.*

---

## 📁 Repository Structure

```
OnlineDoctorAppointmentSystem/
├── pom.xml
├── README.md
├── CHANGELOG.md
├── config.example
├── db/
│   └── seed_data.sql
├── .github/
│   └── workflows/
│       └── build.yml
└── src/
    ├── main/
    │   ├── java/com/odas/
    │   │   ├── LoginServlet.java
    │   │   ├── SignupServlet.java
    │   │   ├── LogoutServlet.java
    │   │   ├── ProfileServlet.java
    │   │   ├── ChangePasswordServlet.java
    │   │   ├── AppointmentServlet.java
    │   │   ├── AppointmentActionServlet.java
    │   │   ├── DoctorServlet.java
    │   │   ├── AdminDoctorServlet.java
    │   │   ├── AdminPatientServlet.java
    │   │   ├── AdminDashboardServlet.java
    │   │   ├── PatientDashboardServlet.java
    │   │   ├── PaymentServlet.java
    │   │   ├── PrescriptionServlet.java
    │   │   ├── dao/
    │   │   │   ├── UserDAO.java
    │   │   │   ├── DoctorDAO.java
    │   │   │   ├── PatientDAO.java
    │   │   │   ├── AppointmentDAO.java
    │   │   │   ├── PaymentDAO.java
    │   │   │   └── PrescriptionDAO.java
    │   │   ├── notify/
    │   │   │   ├── ReminderScheduler.java
    │   │   │   └── NotificationService.java
    │   │   └── util/
    │   │       ├── AuthFilter.java
    │   │       ├── CsrfFilter.java
    │   │       ├── DBConnection.java
    │   │       ├── PasswordUtil.java
    │   │       ├── TimeSlotUtil.java
    │   │       └── ValidationUtil.java
    │   ├── resources/
    │   │   ├── schema.sql
    │   │   └── db.properties.example
    │   └── webapp/
    │       ├── login.jsp
    │       ├── doctors.jsp
    │       ├── appointment.jsp
    │       ├── patient-dashboard.jsp
    │       ├── patient-profile.jsp
    │       ├── doctor-dashboard.jsp
    │       ├── admin-dashboard.jsp
    │       ├── payment.jsp
    │       ├── payment-receipt.jsp
    │       ├── prescription-form.jsp
    │       ├── prescription-view.jsp
    │       ├── change-password.jsp
    │       └── WEB-INF/web.xml
    └── test/java/com/odas/   # 19 test classes (121 tests)
```

---

## 🧭 Roadmap

- Separate Departments management screen (specialization currently serves as the department)
- Doctor-side interactive calendar view
- Real payment gateway integration (Razorpay / Stripe)
- Docker Compose setup for automated Oracle XE and Tomcat containerization

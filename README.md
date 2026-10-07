# Online Doctor Appointment System (ODAS)

A professional, enterprise-grade Java web application for scheduling, managing, and fulfilling clinical consultations between patients and medical specialists. Built on **Jakarta EE 10 / Servlet API 6.1.0**, running on **Apache Tomcat 10.1**, compiled on **JDK 21**, and backed by **Oracle Database 21c Express Edition (XE)** via JDBC (`ojdbc11`).

---

## 1. Project Overview & Key Features

The **Online Doctor Appointment System (ODAS)** provides an end-to-end digital hospital management workflow:

1. **Authentication & Role-Based Access Control (RBAC)**:
   - Secure sign-in and patient registration with SHA-256 cryptographic password hashing.
   - Session management and protection via `AuthFilter` enforcing role boundaries (`PATIENT`, `DOCTOR`, `ADMIN`).
   - Profile management (`patient-profile.jsp`) and password change functionality (`change-password.jsp`) for all roles.
2. **Physician Directory & Advanced Search**:
   - Multi-attribute doctor filtering by name, specialization, hospital location/wing, and available day.
   - Detailed physician profiles with qualifications, clinical experience (years), weekly schedules, and consultation fees.
3. **Intelligent 30-Minute Time Slot Booking**:
   - Dynamic 30-minute interval slot generation (`09:00`, `09:30`, `10:00`, etc.) based on each doctor's specific operating hours.
   - Real-time double-booking prevention ignoring cancelled visits via an Oracle function-based unique slot index (`UQ_ACTIVE_APPT_SLOT`).
   - In-place appointment rescheduling allowing patients to pick a new date or slot with zero conflict risk.
4. **24-Hour Clinical Reminders & Notifications (FR5, FR6, FR7)**:
   - Automated in-app notification banners displayed on the home page and patient dashboard for upcoming visits within the next 24 hours.
   - Multi-channel notification pipeline (SMS + Email) dispatching booking confirmations, rescheduling updates, cancellations, and 24-hr reminder alerts.
   - Pluggable SMS architecture (`MockSmsProvider` for dev/testing, `TwilioSmsProvider` using Twilio REST API with +91 E.164 formatting).
   - Jakarta Mail integration over SMTP with STARTTLS for automated email alerts.
   - Automated background scheduler (`ReminderScheduler` `@WebListener`) checking upcoming appointments every 15 minutes and marking `REMINDER_SENT = 1`.
   - Complete fault tolerance: notification failures run on background executors, never block HTTP responses, and never break booking, cancel, or payment operations.
   - Strict healthcare privacy compliance: messages omit medical diagnoses and conditions.
5. **Electronic Medical Prescriptions**:
   - Exclusive physician action on completed appointments to generate official prescriptions with clinical diagnosis and drug regimens.
   - Clean, printable hospital letterhead layout (`prescription-view.jsp`) with CSS print media queries.
   - Unique constraint enforcement preventing duplicate prescriptions per consultation.
6. **Simulated Online Payment & Receipts**:
   - Atomic database transactions (`conn.setAutoCommit(false)`) recording successful payment and updating appointment payment status simultaneously.
   - Server-enforced consultation fee reading directly from the database to prevent client tampering.
   - Printable official payment receipts (`payment-receipt.jsp`) with demo disclosure.
7. **Hospital Management & Analytical Reports**:
   - Interactive administrative reports on `admin-dashboard.jsp`:
     - **Consultations Report**: Filter by date range and status with matching summary pills.
     - **Payments & Revenue Report**: Filter by date range and payment mode (`Card`, `UPI`, `Net Banking`) with revenue totals and breakdown chips.
     - **Registered Patients Directory**: Patient demographic listings with dynamically aggregated consultation booking counts.
     - **Doctors & Workload Report**: Specialist listings with operating hours, fees, and consultation workload metrics.

---

## 2. Technology Stack & Specifications

| Layer / Component | Technology | Version | Purpose |
|---|---|---|---|
| **Programming Language** | Java (JDK) | JDK 21 (LTS) | Core backend logic & business models |
| **Servlet Specification** | Jakarta Servlet API | 6.1.0 (Jakarta EE 10) | HTTP controllers & authentication filters |
| **Template Engine** | Jakarta Server Pages (JSP) + JSTL | 3.0.0 | Dynamic server-side UI views |
| **Web Server** | Apache Tomcat | 10.1.x | Servlet container (port 8080) |
| **Database** | Oracle Database Express Edition | 21c XE (PDB `XEPDB1`) | Relational persistence & constraints |
| **JDBC Driver** | Oracle JDBC (`ojdbc11`) | 21.11.0.0 | High-performance database connectivity |
| **Email Delivery** | Jakarta Mail + Angus Mail | 2.1.3 / 2.0.3 | Automated SMTP alerts with STARTTLS |
| **SMS Delivery** | Twilio REST API + Mock Provider | JDK `HttpClient` | Automated SMS notifications (E.164 +91) |
| **Build & Dependency Tool** | Apache Maven | 3.9.x | Project lifecycle, testing & WAR packaging |
| **Unit & Integration Tests** | JUnit | 4.13.2 | Automated test suite (121 tests) |
| **Frontend Styling** | CSS3 & Inter Font | HTML5 / CSS3 | Responsive healthcare design system |

---

## 3. Database Architecture & Schema

The relational schema is defined in [`src/main/resources/schema.sql`](file:///c:/Users/HP/OneDrive/Desktop/SOFTWARE%20DEVELOPMENT/OnlineDoctorAppointmentSystem/onlinedoctorappointmentsystem/src/main/resources/schema.sql):

```mermaid
erDiagram
    USERS ||--o| DOCTORS : "authenticates"
    USERS ||--o| PATIENTS : "authenticates"
    PATIENTS ||--o{ APPOINTMENTS : "books"
    DOCTORS ||--o{ APPOINTMENTS : "attends"
    APPOINTMENTS ||--o| PRESCRIPTIONS : "has"
    APPOINTMENTS ||--o| PAYMENTS : "billed_via"

    USERS {
        NUMBER ID PK
        VARCHAR2 USERNAME UK
        VARCHAR2 PASSWORD
        VARCHAR2 ROLE
    }
    DOCTORS {
        NUMBER ID PK
        VARCHAR2 NAME
        VARCHAR2 SPECIALIZATION
        NUMBER USER_ID FK
        VARCHAR2 QUALIFICATION
        NUMBER EXPERIENCE
        VARCHAR2 PHONE
        VARCHAR2 EMAIL
        VARCHAR2 LOCATION
        VARCHAR2 AVAILABLE_DAYS
        VARCHAR2 AVAILABLE_TIME
        NUMBER CONSULTATION_FEE
    }
    PATIENTS {
        NUMBER ID PK
        VARCHAR2 NAME
        NUMBER AGE
        NUMBER USER_ID FK
        VARCHAR2 GENDER
        VARCHAR2 PHONE
        VARCHAR2 EMAIL
        VARCHAR2 ADDRESS
    }
    APPOINTMENTS {
        NUMBER ID PK
        NUMBER DOCTOR_ID FK
        NUMBER PATIENT_ID FK
        TIMESTAMP APPOINTMENT_DATE
        VARCHAR2 STATUS
        VARCHAR2 APPOINTMENT_TIME
        VARCHAR2 PAYMENT_STATUS
        NUMBER REMINDER_SENT
    }
    PRESCRIPTIONS {
        NUMBER ID PK
        NUMBER APPOINTMENT_ID FK_UK
        NUMBER DOCTOR_ID FK
        NUMBER PATIENT_ID FK
        VARCHAR2 DIAGNOSIS
        CLOB PRESCRIPTION_DETAILS
        TIMESTAMP CREATED_AT
    }
    PAYMENTS {
        NUMBER ID PK
        NUMBER APPOINTMENT_ID FK
        NUMBER PATIENT_ID FK
        NUMBER AMOUNT
        VARCHAR2 PAYMENT_MODE
        VARCHAR2 PAYMENT_STATUS
        TIMESTAMP PAYMENT_DATE
    }
```

### Key Relational Constraints & Indexes:
1. **`UQ_ACTIVE_APPT_SLOT`**: Function-based unique index on `(DOCTOR_ID, TRUNC(APPOINTMENT_DATE), APPOINTMENT_TIME)` where `STATUS <> 'CANCELLED'`. Allows cancelled slots to be immediately rebooked while strictly preventing double bookings.
2. **`UQ_PRESCRIPTION_APPT`**: Unique constraint on `PRESCRIPTIONS(APPOINTMENT_ID)`. Enforces exactly one official prescription per completed consultation.
3. **RESTRICT Foreign Keys (No Cascade Deletion)**: Foreign keys on `APPOINTMENTS(doctor_id, patient_id)`, `PRESCRIPTIONS`, and `PAYMENTS` enforce RESTRICT behavior (no cascade delete). Deleting a physician with linked consultation, prescription, or payment records is prevented (`ORA-02292`), preserving clinical history and preventing accidental data destruction.

---

## 4. Oracle 21c XE Setup Guide

### Step 1: Create Schema User via SQL*Plus
Connect as `SYSDBA` to the Pluggable Database (`XEPDB1`):

```sh
sqlplus sys/your_sys_password@localhost:1521/xepdb1 as sysdba
```

Execute the following commands:
```sql
CREATE USER odas_user IDENTIFIED BY odas123;
GRANT CONNECT, RESOURCE TO odas_user;
ALTER USER odas_user QUOTA UNLIMITED ON USERS;
EXIT;
```

### Step 2: Run `schema.sql`
Run the schema script to initialize all tables, sequences, constraints, and baseline seed accounts:

```sh
sqlplus odas_user/odas123@localhost:1521/xepdb1 @"src/main/resources/schema.sql"
```

### Step 2b: Run `seed_data.sql` (Comprehensive Realistic Data Extension)
To populate an extended dataset with 30 medical specialists across 18 clinical departments and 6 cities, 73 patients, 155 appointments, 80 prescriptions, and 125 payments:

```sh
sqlplus odas_user/odas123@localhost:1521/xepdb1 @"seed_data.sql"
```

*Note: `seed_data.sql` uses idempotent `MERGE INTO` statements wrapped in a transaction with rollback on error, making it completely safe to re-run multiple times.*

### Step 3: Configure `db.properties`
Verify that `src/main/resources/db.properties` matches your local database:
```properties
db.driver=oracle.jdbc.OracleDriver
db.url=jdbc:oracle:thin:@localhost:1521/xepdb1
db.username=odas_user
db.password=odas123
```

---

## 5. Build, Test, and Packaging

The project utilizes Maven for dependency resolution and testing:

```sh
# Run all automated tests (68 tests)
mvn clean test

# Package into WAR file
mvn package -DskipTests
```

The resulting artifact is created at:
`target/onlinedoctorappointmentsystem.war`

---

## 6. Apache Tomcat 10.1 Deployment

1. Ensure Apache Tomcat 10.1 is running on port 8080.
2. Deploy the WAR file:
   ```powershell
   Copy-Item "target\onlinedoctorappointmentsystem.war" "C:\path\to\tomcat\webapps\" -Force
   ```
3. Tomcat will automatically unpack the application at `/onlinedoctorappointmentsystem`.

---

## 7. Pre-Configured Accounts for Testing

All test accounts use industry-standard **PBKDF2WithHmacSHA256** (210,000 iterations with 16-byte cryptographically secure salts).

| Role | Username | Password | Specialist / Profile Details |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | Hospital Administrator (Executive Dashboards, User & Report Management) |
| **Doctor** | `dr_kumar` | `doctor123` | Dr. Rajesh Kumar — Cardiologist (Apollo Block A, Floor 2; ₹700.00; `09:00-13:00`) |
| **Doctor** | `dr_priya` | `doctor123` | Dr. Priya Nair — Dermatologist (SkinCare Wing, Suite 101; ₹600.00; `10:00-14:00`) |
| **Doctor** | `dr_sharma` | `doctor123` | Dr. Vikram Sharma — Pediatrician (Children Health Centre, Block B; ₹500.00; `14:00-18:00`) |
| **Doctor** | `dr_ananya` | `doctor123` | Dr. Ananya Sen — Neurologist (Neuro Sciences Wing, Floor 3; ₹900.00; `10:00-14:00`) |
| **Doctor** | `dr_patel` | `doctor123` | Dr. Manoj Patel — Orthopedic Surgeon (Bone & Joint Clinic, Block C; ₹800.00; `09:00-13:00`) |
| **Doctor** | `dr_meera` | `doctor123` | Dr. Meera Reddy — Gynecologist (Women Care Centre, Floor 2; ₹650.00; `11:00-15:00`) |
| **Doctor** | `dr_suresh` | `doctor123` | Dr. Suresh Verma — General Physician (Primary Care OPD, Ground Floor; ₹400.00; `08:30-12:30`) |
| **Doctor** | `dr_neha` | `doctor123` | Dr. Neha Gupta — Ophthalmologist (Eye Care Pavilion, Block B; ₹550.00; `13:00-17:00`) |
| **Doctor** | `dr_arjun` | `doctor123` | Dr. Arjun Deshmukh — ENT Specialist (Head & Neck Clinic, Floor 1; ₹500.00; `09:30-13:30`) |
| **Doctor** | `dr_kavita` | `doctor123` | Dr. Kavita Iyer — Psychiatrist (Mind & Wellness Centre, Floor 4; ₹850.00; `15:00-19:00`) |
| **Doctor** | `dr_rohan` | `doctor123` | Dr. Rohan Joshi — Endocrinologist (Diabetes & Hormone Centre, Floor 2; ₹750.00; `10:00-14:00`) |
| **Doctor** | `dr_sneha` | `doctor123` | Dr. Sneha Mukherjee — Pulmonologist (Chest & Respiratory Clinic, Block A; ₹700.00; `11:00-15:00`) |
| **Doctor (New)** | `dr_vikram` | `doctor123` | Dr. Vikram Malhotra — Dentistry (SmileCraft Dental Wing, Indiranagar, Bangalore; ₹450.00; `09:00-13:00`, `Mon,Wed,Fri`) |
| **Doctor (New)** | `dr_aditi` | `doctor123` | Dr. Aditi Rao — Urology (Apex Uro-Care Centre, Banjara Hills, Hyderabad; ₹850.00; `14:00-18:00`, `Tue,Thu,Sat`) |
| **Doctor (New)** | `dr_kunal` | `doctor123` | Dr. Kunal Singhania — Oncology (Tata Cancer Pavilion, Parel, Mumbai; ₹1100.00; `08:30-12:30`, `Mon,Tue,Thu,Fri`) |
| **Doctor (New)** | `dr_shalini` | `doctor123` | Dr. Shalini Nair — Nephrology (Kidney Care Institute, Saket, Delhi; ₹950.00; `10:00-14:00`, `Mon,Wed,Sat`) |
| **Doctor (New)** | `dr_tarun` | `doctor123` | Dr. Tarun Sengupta — Gastroenterology (Gastro Digestive Clinic, Shivajinagar, Pune; ₹800.00; `15:00-19:00`, `Tue,Thu,Fri`) |
| **Doctor (New)** | `dr_pooja_m` | `doctor123` | Dr. Pooja Menon — Cardiology (Metro Heart Pavilion, Salt Lake, Kolkata; ₹900.00; `09:30-13:30`, `Mon,Tue,Wed,Thu,Fri`) |
| **Doctor (New)** | `dr_rajesh` | `doctor123` | Dr. Rajesh Nambiar — Orthopedics (Joint & Spine Care, Koramangala, Bangalore; ₹750.00; `14:30-18:30`, `Mon,Wed,Fri`) |
| **Doctor (New)** | `dr_meenakshi` | `doctor123` | Dr. Meenakshi Sundaram — Pediatrics (Little Stars Children Clinic, Jubilee Hills, Hyderabad; ₹500.00; `10:00-14:00`, `Tue,Thu,Sat`) |
| **Doctor (New)** | `dr_vivek` | `doctor123` | Dr. Vivek Vardhan — Neurology (Brain & Nerve Centre, Bandra West, Mumbai; ₹1000.00; `15:30-19:30`, `Mon,Tue,Thu`) |
| **Doctor (New)** | `dr_shweta` | `doctor123` | Dr. Shweta Kulkarni — Dermatology (Derma Glow Aesthetic Centre, Vasant Kunj, Delhi; ₹650.00; `11:00-15:00`, `Wed,Thu,Fri`) |
| **Doctor (New)** | `dr_arvind` | `doctor123` | Dr. Arvind Swaminathan — Ophthalmology (Netra Eye Care Hospital, Aundh, Pune; ₹550.00; `09:00-13:00`, `Mon,Wed,Fri`) |
| **Doctor (New)** | `dr_ritika` | `doctor123` | Dr. Ritika Banerjee — ENT (Sound & Voice ENT Clinic, Park Street, Kolkata; ₹600.00; `14:00-18:00`, `Tue,Thu,Sat`) |
| **Doctor (New)** | `dr_hariprasad` | `doctor123` | Dr. Hariprasad Hegde — Psychiatry (Manas Mind Wellness Clinic, Jayanagar, Bangalore; ₹850.00; `16:00-20:00`, `Mon,Tue,Thu,Fri`) |
| **Doctor (New)** | `dr_vandana` | `doctor123` | Dr. Vandana Saxena — General Medicine (City Care Family OPD, Somajiguda, Hyderabad; ₹400.00; `08:00-12:00`, `Mon,Tue,Wed,Thu,Fri`) |
| **Doctor (New)** | `dr_siddharth` | `doctor123` | Dr. Siddharth Goswami — Endocrinology (Hormone & Thyroid Clinic, Andheri East, Mumbai; ₹750.00; `10:30-14:30`, `Tue,Thu,Sat`) |
| **Doctor (New)** | `dr_aparna` | `doctor123` | Dr. Aparna Sen — Pulmonology (Breath Easy Pulmonary Care, Connaught Place, Delhi; ₹700.00; `15:00-19:00`, `Mon,Wed,Fri`) |
| **Doctor (New)** | `dr_farhan` | `doctor123` | Dr. Farhan Qureshi — Rheumatology (Arthritis & Rheuma Clinic, Kothrud, Pune; ₹800.00; `09:30-13:30`, `Tue,Thu,Sat`) |
| **Doctor (New)** | `dr_deepali` | `doctor123` | Dr. Deepali Deshpande — Plastic Surgery (Aesthetic Reconstructive Suite, Ballygunge, Kolkata; ₹1200.00; `14:00-18:00`, `Mon,Wed,Fri`) |
| **Patient** | `rahul1` | `password123` | Rahul Verma (29 yrs, Male; Chennai; Consultation history with Cardiologist & Physician) |
| **Patient** | `anita_m` | `password123` | Anita Menon (34 yrs, Female; Chennai; Consultation history with Dermatologist) |
| **Patient** | `karthik_r` | `password123` | Karthik Ramaswamy (42 yrs, Male; Chennai; Consultation history with Orthopedic Surgeon) |
| **Patient** | `deepa_s` | `password123` | Deepa Sundaram (27 yrs, Female; Chennai; Consultation history with Gynecologist) |
| **Patient** | `sanjay_v` | `password123` | Sanjay Venkat (51 yrs, Male; Chennai; Consultation history with Neurologist) |
| **Patient (New)** | `karan_s` | `password123` | Karan Sharma (31 yrs, Male; Bangalore; Consultation history with Dr. Vikram Malhotra) |
| **Patient (New)** | `neha_v` | `password123` | Neha Varma (28 yrs, Female; Mumbai; Consultation history with Dr. Kunal Singhania) |
| **Patient (New)** | `rohit_p` | `password123` | Rohit Pandey (35 yrs, Male; New Delhi; Consultation history with Dr. Shalini Nair) |
| **Patient (New)** | `ananya_m` | `password123` | Ananya Mishra (26 yrs, Female; Hyderabad; Consultation history with Dr. Aditi Rao) |

*(Additional 64 pre-seeded patient accounts across children, adults, and seniors all use password `password123`. New patients can also register self-service via `/signup.jsp`.)*

---

## 8. Complete Step-by-Step Testing Guide

### Test 1: Patient Profile & Password Rotation (Stage 2)
1. Log in as `eniya` / `password123`.
2. Click **"My Profile"** in the navigation bar.
3. Update demographic info (Age, Phone, Address) and submit. Verify persistent update.
4. Click **"Change Password"**, enter current password and a new 6+ character password, and submit.
5. Log out and log back in with the new password to confirm.

### Test 2: Multi-Attribute Doctor Search (Stage 3)
1. Navigate to **"Find Doctors"** (`/doctors`).
2. Search by:
   - Doctor Name: e.g. `Kumar`
   - Specialization: e.g. `Cardiology`
   - Available Day: e.g. `Mon`
3. Observe real-time filtered results, qualification chips, and consultation fee tags. Click **"Reset Filters"** to restore.

### Test 3: 30-Minute Time Slot Booking & Rescheduling (Stage 4)
1. From the doctors directory, click **"Book Appointment"** on *Dr. Kumar*.
2. Notice operating hours (`09:30-13:30`) and select tomorrow's date.
3. Observe available 30-minute slots: `09:30 AM`, `10:00 AM`, `10:30 AM`, etc.
4. Select `10:00 AM` and confirm booking.
5. In your dashboard, click **"Reschedule"**, select a different time slot (e.g. `11:00 AM`), and submit. Confirm updated slot.

### Test 4: 24-Hour Clinical Reminders (Stage 4)
1. Book an appointment for today's date or tomorrow morning within 24 hours.
2. Visit the **Home Page** or **Patient Dashboard**.
3. Observe the prominent amber banner: *"Upcoming Consultation Reminder: You have an appointment with Dr. Kumar scheduled within the next 24 hours"*.

### Test 5: Clinical Prescriptions (Stage 5 & 5.5)
1. Log in as `dr_kumar` / `doctor123`.
2. Locate a scheduled consultation and click **"Mark Completed"**.
3. Click **"💊 Add Prescription"**, enter diagnosis (e.g. *"Hypertension Stage 1"*) and multi-line pharmaceutical regimen with instructions.
4. Submit and observe the prescription letterhead view (`prescription-view.jsp`).
5. Attempting to add a duplicate prescription redirects directly to the existing prescription record.

### Test 6: Simulated Online Payments & Receipts (Stage 6)
1. Log in as `eniya`. In the dashboard, locate an unpaid appointment and click **"💳 Pay Now"**.
2. Confirm the fee amount comes from the database (e.g. ₹650.00) and choose a payment mode (`Card`, `UPI`, `Net Banking`).
3. Note the simulation banner: *"Demo / simulated payment. No real money is charged"*.
4. Submit payment. Observe atomic commit: appointment changes to `PAID` and receipt `#PAY-...` is generated.
5. Click **"🧾 Paid (Receipt)"** to open the printable receipt.

### Test 7: Admin Reports & Analytics (Stage 7)
1. Log in as `admin` / `admin123`.
2. View top metric cards: Registered Doctors, Consultations, Patients, and **Total Revenue** (₹).
3. Switch between tabs:
   - **📅 Consultations Report**: Filter by date range and status (`SCHEDULED`, `COMPLETED`, `CANCELLED`).
   - **💳 Payments & Revenue**: Filter by payment mode and inspect Card/UPI/Net Banking revenue breakdown.
   - **👥 Registered Patients**: View all patients and their total bookings count.
   - **👨‍⚕️ Doctors & Workload**: View active physicians with schedule hours, fees, and consultation counts.

### Test 8: Appointment Notifications & 24h Background Reminders (FR5, FR6, FR7)
1. Book an appointment as `eniya` with Dr. Kumar for tomorrow.
2. In the application console / Tomcat logs, observe the non-blocking mock SMS dispatch:
   `[MOCK SMS] to=9876543210 msg=ODAS: Your appointment with Dr. Kumar is on YYYY-MM-DD at HH:MM AM. Reply not required.`
3. Reschedule the appointment to a different slot:
   Observe the confirmation dispatch with the updated date/time. Notice that `REMINDER_SENT` is reset to 0 in Oracle DB.
4. Cancel the appointment:
   Observe the cancellation dispatch:
   `[MOCK SMS] to=9876543210 msg=ODAS: Your appointment with Dr. Kumar on YYYY-MM-DD at HH:MM AM has been cancelled.`
5. Background Reminder Verification:
   The `ReminderScheduler` runs every 15 minutes, finds appointments within 24 hours where `REMINDER_SENT = 0`, dispatches the reminder, and sets `REMINDER_SENT = 1`.

---

## 9. Notification Configuration & Environment Variables

The notification engine is configured exclusively through environment variables without storing secrets or API tokens in source code:

| Variable | Values / Default | Description |
|---|---|---|
| `ODAS_SMS_PROVIDER` | `mock` (default) \| `twilio` | Provider implementation (`MockSmsProvider` vs `TwilioSmsProvider`). |
| `TWILIO_ACCOUNT_SID` | E.g. `AC...` | Twilio Account SID (required when `ODAS_SMS_PROVIDER=twilio`). |
| `TWILIO_AUTH_TOKEN` | Secret string | Twilio Auth Token (used for HTTP Basic Auth with JDK `HttpClient`). |
| `TWILIO_FROM_NUMBER` | E.g. `+1234567890` | Approved Twilio phone number sending outbound SMS. |
| `ODAS_SMTP_HOST` | E.g. `smtp.gmail.com` | SMTP relay server hostname. If omitted, email is silently skipped with an `INFO` log. |
| `ODAS_SMTP_PORT` | E.g. `587` | SMTP port (STARTTLS default: 587). |
| `ODAS_SMTP_USER` | E.g. `clinic@hospital.com` | SMTP username for authentication. |
| `ODAS_SMTP_PASSWORD` | App-specific password | SMTP secret password / application token. |

*Note: For Indian phone numbers, 10-digit mobile numbers are automatically normalized to international E.164 format with prefix `+91`.*

---

## 10. Viva / Defense Preparation Points

1. **Why Oracle Database 21c XE instead of MySQL?**
   - Enterprise-grade ACID transaction guarantees, sequence-based and identity columns, native `TIMESTAMP` precision, built-in `NVL` / `TRUNC` functions, and powerful function-based indexing (`UQ_ACTIVE_APPT_SLOT`).
2. **How is SQL Injection prevented?**
   - Strict use of `PreparedStatement` with parameterized placeholders (`?`) across all DAO queries (`UserDAO`, `DoctorDAO`, `PatientDAO`, `AppointmentDAO`, `PrescriptionDAO`, `PaymentDAO`). No user string concatenation is used.
3. **How does the simulated payment transaction maintain atomicity?**
   - Managed via `conn.setAutoCommit(false)` in `PaymentDAO.processPaymentTransaction()`. The insert into `PAYMENTS` and the update to `APPOINTMENTS.PAYMENT_STATUS = 'PAID'` happen in the same database transaction. If either operation fails, `conn.rollback()` restores the initial state.
4. **How is Role-Based Access Control (RBAC) enforced?**
   - Implemented via a central Jakarta `AuthFilter` checking `session.getAttribute("role")`. Unauthenticated requests redirect to `login.jsp?error=unauthorized`, and cross-role resource access is rejected with HTTP redirects.
5. **How are XSS vulnerabilities prevented in prescription details?**
   - Output rendered through JSTL `<c:out value="${...}"/>` escaping raw HTML, paired with CSS `white-space: pre-wrap;` to faithfully preserve formatted medical dosage instructions and line breaks.
6. **How is the notification system decoupled from core appointment booking?**
   - `NotificationService` dispatches messages on a background daemon executor pool (`asyncExecutor.submit(...)`) and catches all `Throwable` instances internally. A network outage or provider error (Twilio/SMTP) will never throw an exception or interrupt the booking, rescheduling, or payment flow.
7. **How does the 24-hour reminder scheduler work without database locks?**
   - `ReminderScheduler` is a Jakarta `@WebListener` (`ServletContextListener`) running a `ScheduledExecutorService` every 15 minutes. It executes a parameterized range query on `APPOINTMENTS` for `STATUS = 'SCHEDULED' AND REMINDER_SENT = 0 AND APPOINTMENT_DATE BETWEEN now AND now + 24 hours`. It marks `REMINDER_SENT = 1` only upon channel success, and rescheduling automatically resets `REMINDER_SENT = 0`.
8. **How is patient healthcare privacy protected in automated messages?**
   - Notification templates strictly contain administrative visit logistics (Physician Name, Date, Time Slot, and ODAS branding). Sensitive clinical notes, diagnoses, medications, and symptoms are never included in outbound SMS or email payloads.


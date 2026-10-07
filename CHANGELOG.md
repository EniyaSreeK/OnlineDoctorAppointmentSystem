# Changelog

All notable changes to the **Online Doctor Appointment System (ODAS)** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-10-07

### Added
- **Comprehensive Realistic Seed Dataset (`seed_data.sql`)**:
  - 18 new medical specialists across 18 departments (Dentistry, Urology, Oncology, Nephrology, Gastroenterology, Cardiology, Orthopedics, Pediatrics, Neurology, Dermatology, Ophthalmology, ENT, Psychiatry, General Medicine, Endocrinology, Pulmonology, Rheumatology, Plastic Surgery) located across 6 metropolitan cities (Bangalore, Mumbai, Delhi, Hyderabad, Pune, Kolkata).
  - 45 new registered patient accounts with authentic demographics across children (ages 6–17), adults (ages 25–49), and seniors (ages 62–76).
  - 80 linked consultations (42 Completed, 28 Scheduled, 10 Cancelled) complying with active slot uniqueness constraints (`UQ_ACTIVE_APPT_SLOT`, `UQ_ACTIVE_PATIENT_SLOT`).
  - 42 diagnostic prescriptions for completed consultations with clinical regimens.
  - 69 payment transactions covering Card, UPI, and Net Banking across `SUCCESS`, `PENDING`, and `REFUNDED` statuses.
  - Idempotent `MERGE INTO` implementation with transactional rollback safety.
- **Continuous Integration (`.github/workflows/build.yml`)**:
  - Automated GitHub Actions CI workflow compiling on JDK 21 and executing the Maven test suite on pushes and pull requests.
- **Configuration Templates & Security Hardening**:
  - Added `config.example` and `src/main/resources/db.properties.example` with credential placeholders.
  - Enhanced `DBConnection` to check `db.local.properties`, `application.local.properties`, and `db.properties` with fallback to environment variables.
  - Updated `.gitignore` to protect all local config files, WAR packages, logs, and IDE metadata.

### Changed
- **Doctor Consultation Management (`BUG-1`)**:
  - Enabled attending physicians to cancel or decline scheduled visits directly from `doctor-dashboard.jsp` with confirmation prompts and automated patient notification dispatch.
- **Administrative Patient Management (`FR2 gap`)**:
  - Added patient editing capabilities in Admin portal (`AdminPatientServlet`) with strict validation, duplicate email/phone checks, and CSRF token protection.
- **Hospital Analytics & Reports (`FR11 gap`)**:
  - Added date range filtering and specialization querying for Registered Patients and Registered Doctors in `admin-dashboard.jsp`.
- **UI Enhancements (`BUG-2`, `BUG-3`)**:
  - Fixed null booking count display in `admin-dashboard.jsp`.
  - Replaced corrupted placeholder characters with SVG icons in patient views.
  - Enforced UTF-8 page encoding and Maven build source encoding across all modules.

---

## [1.1.0] - 2026-09-24

### Added
- Automated notifications pipeline (FR5, FR6, FR7) with pluggable SMS providers (`MockSmsProvider`, `TwilioSmsProvider`) and SMTP email dispatch (`EmailSender`).
- 24-hour clinical reminder scheduler (`ReminderScheduler` background executor).
- Simulated payment processing with printable receipts (`PaymentServlet`, `payment-receipt.jsp`).
- Clinical prescription letterhead generator (`PrescriptionServlet`, `prescription-view.jsp`).

---

## [1.0.0] - 2026-09-15

### Added
- Initial core architecture: Jakarta EE Servlet controllers, Oracle 21c XE database schema (`schema.sql`), and authentication models (`USERS`, `DOCTORS`, `PATIENTS`, `APPOINTMENTS`).
- 30-minute dynamic appointment scheduling engine and conflict prevention.

package com.odas.notify;

import com.odas.Appointment;
import com.odas.Doctor;
import com.odas.Patient;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
import com.odas.util.TimeSlotUtil;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized notification service coordinating SMS and Email dispatch for appointments.
 * 
 * Purpose (for viva):
 * - Dispatches booking, rescheduling, cancellation, and 24-hour reminder notifications (FR5, FR6, FR7).
 * - Pluggable SMS architecture (MockSmsProvider for dev/testing, TwilioSmsProvider for production).
 * - Enforces transactional resilience: never throws exceptions so notifications never block core flows.
 * - Provides asynchronous background dispatch to keep servlet HTTP responses instantaneous.
 * - Enforces healthcare privacy standards: messages omit medical conditions and diagnoses.
 */
public class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    private static volatile NotificationService defaultInstance;

    private final SmsProvider smsProvider;
    private final EmailSender emailSender;
    private final PatientDAO patientDAO;
    private final DoctorDAO doctorDAO;
    private final ExecutorService asyncExecutor;

    public static NotificationService getInstance() {
        if (defaultInstance == null) {
            synchronized (NotificationService.class) {
                if (defaultInstance == null) {
                    defaultInstance = new NotificationService();
                }
            }
        }
        return defaultInstance;
    }

    public static void setInstance(NotificationService instance) {
        synchronized (NotificationService.class) {
            defaultInstance = instance;
        }
    }

    public NotificationService() {
        this(
            resolveSmsProvider(),
            new EmailSender(),
            new PatientDAO(),
            new DoctorDAO(),
            Executors.newCachedThreadPool()
        );
    }

    public NotificationService(SmsProvider smsProvider, EmailSender emailSender, 
                               PatientDAO patientDAO, DoctorDAO doctorDAO) {
        this(smsProvider, emailSender, patientDAO, doctorDAO, Executors.newCachedThreadPool());
    }

    public NotificationService(SmsProvider smsProvider, EmailSender emailSender, 
                               PatientDAO patientDAO, DoctorDAO doctorDAO, 
                               ExecutorService asyncExecutor) {
        this.smsProvider = (smsProvider != null) ? smsProvider : new MockSmsProvider();
        this.emailSender = (emailSender != null) ? emailSender : new EmailSender();
        this.patientDAO = (patientDAO != null) ? patientDAO : new PatientDAO();
        this.doctorDAO = (doctorDAO != null) ? doctorDAO : new DoctorDAO();
        this.asyncExecutor = (asyncExecutor != null) ? asyncExecutor : Executors.newCachedThreadPool();
    }

    /**
     * Resolves SMS provider based on ODAS_SMS_PROVIDER environment variable ('mock' vs 'twilio').
     */
    private static SmsProvider resolveSmsProvider() {
        String provider = System.getenv("ODAS_SMS_PROVIDER");
        if (provider == null || provider.trim().isEmpty()) {
            provider = System.getProperty("ODAS_SMS_PROVIDER");
        }
        if ("twilio".equalsIgnoreCase(provider)) {
            LOGGER.info("NotificationService configured with TwilioSmsProvider");
            return new TwilioSmsProvider();
        }
        LOGGER.info("NotificationService configured with MockSmsProvider (default)");
        return new MockSmsProvider();
    }

    // =========================================================================
    // Synchronous Dispatch APIs (returns true if at least one channel succeeded)
    // =========================================================================

    /**
     * Sends booking confirmation via SMS and/or Email.
     * 
     * @param appointment booked appointment
     * @return true if at least one channel succeeded, false otherwise
     */
    public boolean sendBookingConfirmation(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        try {
            String doctorName = resolveDoctorName(appointment);
            String dateStr = resolveDateString(appointment);
            String timeStr = resolveTimeString(appointment);

            String message = "ODAS: Your appointment with Dr. " + doctorName + " is on " + dateStr + " at " + timeStr + ". Reply not required.";
            String subject = "Appointment Confirmation - MediCare ODAS";

            return dispatchNotification(appointment, subject, message);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to send booking confirmation for appointment ID=" + appointment.getId(), t);
            return false;
        }
    }

    /**
     * Sends cancellation notification via SMS and/or Email.
     * 
     * @param appointment cancelled appointment
     * @return true if at least one channel succeeded, false otherwise
     */
    public boolean sendCancellationConfirmation(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        try {
            String doctorName = resolveDoctorName(appointment);
            String dateStr = resolveDateString(appointment);
            String timeStr = resolveTimeString(appointment);

            String message = "ODAS: Your appointment with Dr. " + doctorName + " on " + dateStr + " at " + timeStr + " has been cancelled.";
            String subject = "Appointment Cancellation - MediCare ODAS";

            return dispatchNotification(appointment, subject, message);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to send cancellation confirmation for appointment ID=" + appointment.getId(), t);
            return false;
        }
    }

    /**
     * Sends 24-hour reminder notification via SMS and/or Email.
     * 
     * @param appointment upcoming appointment
     * @return true if at least one channel succeeded, false otherwise
     */
    public boolean sendReminder(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        try {
            String doctorName = resolveDoctorName(appointment);
            String dateStr = resolveDateString(appointment);
            String timeStr = resolveTimeString(appointment);

            String message = "ODAS Reminder: Your appointment with Dr. " + doctorName + " is on " + dateStr + " at " + timeStr + ". Reply not required.";
            String subject = "Appointment Reminder - MediCare ODAS";

            return dispatchNotification(appointment, subject, message);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to send reminder for appointment ID=" + appointment.getId(), t);
            return false;
        }
    }

    // =========================================================================
    // Asynchronous Dispatch APIs (Background Executor)
    // =========================================================================

    public void sendBookingConfirmationAsync(Appointment appointment) {
        if (appointment == null) return;
        asyncExecutor.submit(() -> sendBookingConfirmation(appointment));
    }

    public void sendCancellationConfirmationAsync(Appointment appointment) {
        if (appointment == null) return;
        asyncExecutor.submit(() -> sendCancellationConfirmation(appointment));
    }

    public void sendReminderAsync(Appointment appointment) {
        if (appointment == null) return;
        asyncExecutor.submit(() -> sendReminder(appointment));
    }

    // =========================================================================
    // Internal Helper Dispatch
    // =========================================================================

    private boolean dispatchNotification(Appointment appointment, String subject, String message) {
        Patient patient = resolvePatient(appointment);
        if (patient == null) {
            LOGGER.warning("Patient not found for appointment ID=" + appointment.getId() + "; notification cannot be sent.");
            return false;
        }

        boolean smsSuccess = false;
        boolean emailSuccess = false;

        String phone = patient.getPhone();
        if (phone != null && !phone.trim().isEmpty()) {
            smsSuccess = smsProvider.send(phone.trim(), message);
        }

        String email = patient.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            emailSuccess = emailSender.send(email.trim(), subject, message);
        }

        return smsSuccess || emailSuccess;
    }

    public Patient resolvePatient(Appointment appointment) {
        if (appointment.getPatient() != null) {
            return appointment.getPatient();
        }
        if (appointment.getPatientId() > 0) {
            try {
                return patientDAO.getPatientById(appointment.getPatientId());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error looking up patient ID=" + appointment.getPatientId(), e);
            }
        }
        return null;
    }

    private String resolveDoctorName(Appointment appointment) {
        if (appointment.getDoctorName() != null && !appointment.getDoctorName().trim().isEmpty()) {
            return appointment.getDoctorName().trim().replace("Dr. ", "");
        }
        if (appointment.getDoctorId() > 0) {
            try {
                Doctor doc = doctorDAO.getDoctorById(appointment.getDoctorId());
                if (doc != null && doc.getName() != null) {
                    return doc.getName().replace("Dr. ", "").trim();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error looking up doctor ID=" + appointment.getDoctorId(), e);
            }
        }
        return "Doctor";
    }

    private String resolveDateString(Appointment appointment) {
        if (appointment.getAppointmentDate() != null) {
            try {
                LocalDate ld = appointment.getAppointmentDate().toLocalDateTime().toLocalDate();
                return ld.toString();
            } catch (Exception ignored) {}
        }
        if (appointment.getDate() != null && !appointment.getDate().trim().isEmpty()) {
            String d = appointment.getDate().trim();
            if (d.length() >= 10 && d.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                return d.substring(0, 10);
            }
            return d;
        }
        return LocalDate.now().toString();
    }

    private String resolveTimeString(Appointment appointment) {
        String rawTime = appointment.getAppointmentTime();
        if (rawTime != null && !rawTime.trim().isEmpty()) {
            return TimeSlotUtil.formatSlotLabel(rawTime.trim());
        }
        return "10:00 AM";
    }

    public void shutdown() {
        try {
            asyncExecutor.shutdown();
        } catch (Exception ignored) {}
    }
}

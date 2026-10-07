package com.odas.notify;

import com.odas.Appointment;
import com.odas.Patient;
import com.odas.dao.AppointmentDAO;
import com.odas.util.TimeSlotUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background scheduler (WebListener) that checks for upcoming appointments
 * in the next 24 hours and dispatches reminder notifications (FR7).
 * 
 * Purpose (for viva):
 * - Implements ServletContextListener to start a background daemon thread on webapp deployment.
 * - Queries SCHEDULED appointments within today and tomorrow where REMINDER_SENT = 0.
 * - Accurately filters appointments in Java combining APPOINTMENT_DATE (at 00:00) with APPOINTMENT_TIME.
 * - Marks REMINDER_SENT = 1 if patient lacks phone and email to avoid endless retries.
 * - Dispatches SMS/Email reminders and updates REMINDER_SENT = 1 only if at least one channel succeeds.
 * - Safely terminates background thread pool upon servlet context shutdown to prevent memory leaks.
 */
@WebListener
public class ReminderScheduler implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(ReminderScheduler.class.getName());

    private final AppointmentDAO appointmentDAO;
    private final NotificationService notificationService;
    private ScheduledExecutorService scheduler;

    public ReminderScheduler() {
        this(new AppointmentDAO(), NotificationService.getInstance());
    }

    public ReminderScheduler(AppointmentDAO appointmentDAO, NotificationService notificationService) {
        this.appointmentDAO = (appointmentDAO != null) ? appointmentDAO : new AppointmentDAO();
        this.notificationService = (notificationService != null) ? notificationService : NotificationService.getInstance();
    }

    /**
     * Pure function helper to determine whether an appointment time falls within the reminder window
     * (i.e. now <= appointmentTime <= now + windowHours).
     *
     * @param now current time reference
     * @param appointmentTime time of appointment
     * @param windowHours window duration in hours (e.g. 24)
     * @return true if within [now, now + windowHours], false otherwise
     */
    public static boolean isWithinReminderWindow(LocalDateTime now, LocalDateTime appointmentTime, int windowHours) {
        if (now == null || appointmentTime == null || windowHours <= 0) {
            return false;
        }
        LocalDateTime windowEnd = now.plusHours(windowHours);
        return !appointmentTime.isBefore(now) && !appointmentTime.isAfter(windowEnd);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Starting ODAS ReminderScheduler background service (interval: 15 mins)...");
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "odas-reminder-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        // Run initially after 1 minute, then repeat every 15 minutes
        scheduler.scheduleAtFixedRate(this::processUpcomingReminders, 1, 15, TimeUnit.MINUTES);
    }

    /**
     * Finds upcoming unreminded appointments in the next 24 hours, dispatches reminders,
     * and marks them as sent if at least one notification channel succeeds.
     * 
     * @return count of reminders successfully sent and marked
     */
    public int processUpcomingReminders() {
        return processUpcomingReminders(LocalDateTime.now());
    }

    /**
     * Overload supporting a simulated/fake clock for deterministic unit testing.
     *
     * @param now current reference timestamp
     * @return count of reminders successfully sent and marked
     */
    public int processUpcomingReminders(LocalDateTime now) {
        if (now == null) {
            now = LocalDateTime.now();
        }
        int sentCount = 0;
        try {
            LocalDate today = now.toLocalDate();
            List<Appointment> upcoming = appointmentDAO.getUpcomingUnreminded(today);
            if (!upcoming.isEmpty()) {
                LOGGER.info("ReminderScheduler: Found " + upcoming.size() + " appointment candidate(s) for " + today + " and " + today.plusDays(1));
            }
            for (Appointment appt : upcoming) {
                try {
                    // Combine date + time into LocalDateTime
                    LocalDateTime apptDateTime = null;
                    if (appt.getAppointmentDate() != null) {
                        apptDateTime = TimeSlotUtil.toLocalDateTime(appt.getAppointmentDate(), appt.getAppointmentTime());
                    } else if (appt.getDate() != null && !appt.getDate().trim().isEmpty()) {
                        try {
                            LocalDate d = LocalDate.parse(appt.getDate().trim());
                            LocalTime t = TimeSlotUtil.parseTime(appt.getAppointmentTime());
                            apptDateTime = d.atTime(t);
                        } catch (Exception ignored) {}
                    }

                    if (apptDateTime == null || !isWithinReminderWindow(now, apptDateTime, 24)) {
                        continue;
                    }

                    // Check patient contact information
                    Patient patient = notificationService.resolvePatient(appt);
                    boolean hasPhone = (patient != null && patient.getPhone() != null && !patient.getPhone().trim().isEmpty());
                    boolean hasEmail = (patient != null && patient.getEmail() != null && !patient.getEmail().trim().isEmpty());

                    if (!hasPhone && !hasEmail) {
                        LOGGER.warning("Patient for appointment ID=" + appt.getId() + " has neither phone nor email; marking REMINDER_SENT = 1 to prevent retrying forever.");
                        appointmentDAO.markReminderSent(appt.getId());
                        continue;
                    }

                    boolean success = notificationService.sendReminder(appt);
                    if (success) {
                        boolean marked = appointmentDAO.markReminderSent(appt.getId());
                        if (marked) {
                            sentCount++;
                            LOGGER.info("Successfully sent 24h reminder for appointment ID=" + appt.getId());
                        }
                    } else {
                        LOGGER.warning("Could not dispatch reminder for appointment ID=" + appt.getId() + " on any channel.");
                    }
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "Error processing reminder for appointment ID=" + appt.getId(), t);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Unexpected error occurred during ReminderScheduler execution cycle", t);
        }
        return sentCount;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Shutting down ODAS ReminderScheduler background service...");
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (notificationService != null) {
            notificationService.shutdown();
        }
    }
}

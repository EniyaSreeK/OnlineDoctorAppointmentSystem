package com.odas;

import com.odas.dao.AppointmentDAO;
import com.odas.dao.DoctorDAO;
import com.odas.dao.PatientDAO;
import com.odas.notify.NotificationService;
import com.odas.util.TimeSlotUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AppointmentServlet handles booking and rescheduling appointments between patients and doctors.
 * 
 * Purpose (for viva):
 * - Validates input parameters (no past dates, valid time slot format, doctor selection).
 * - Enforces conflict checking across doctor + date + time slot via AppointmentDAO.
 * - Handles both initial booking and in-place rescheduling without duplicate row creation.
 * - Persists 24-hr time slot string and date to Oracle APPOINTMENTS table.
 */
@WebServlet("/appointment")
public class AppointmentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AppointmentServlet.class.getName());

    private DoctorDAO doctorDAO;
    private PatientDAO patientDAO;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() throws ServletException {
        this.doctorDAO = new DoctorDAO();
        this.patientDAO = new PatientDAO();
        this.appointmentDAO = new AppointmentDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/appointment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Verify patient authentication
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        Integer patientId = (Integer) session.getAttribute("patientId");
        if (patientId == null) {
            // Fallback lookup if not cached in session
            Integer userId = (Integer) session.getAttribute("userId");
            Patient p = patientDAO.getPatientByUserId(userId);
            if (p != null) {
                patientId = p.getId();
                session.setAttribute("patientId", patientId);
                session.setAttribute("patientName", p.getName());
            } else {
                response.sendRedirect("login.jsp?error=unauthorized");
                return;
            }
        }

        String rescheduleIdParam = request.getParameter("rescheduleId");
        boolean isReschedule = (rescheduleIdParam != null && !rescheduleIdParam.trim().isEmpty());

        String doctorName = request.getParameter("doctorName");
        String doctorIdParam = request.getParameter("doctorId");
        String dateStr = request.getParameter("date");
        String timeStr = request.getParameter("appointmentTime");
        if (timeStr == null || timeStr.trim().isEmpty()) {
            timeStr = request.getParameter("time");
        }
        if (timeStr == null || timeStr.trim().isEmpty()) {
            timeStr = "10:00";
        } else {
            timeStr = timeStr.trim();
        }

        // Validate required inputs
        if (!isReschedule && (doctorName == null || doctorName.trim().isEmpty()) && 
            (doctorIdParam == null || doctorIdParam.trim().isEmpty())) {
            sendError(request, response, "Please select a doctor for the appointment.");
            return;
        }

        if (dateStr == null || dateStr.trim().isEmpty()) {
            sendError(request, response, "Please select a valid appointment date.");
            return;
        }

        // Validate date parsing and past-date prohibition
        LocalDate appointmentDate;
        try {
            appointmentDate = LocalDate.parse(dateStr.trim());
            if (appointmentDate.isBefore(LocalDate.now())) {
                sendError(request, response, "Appointments cannot be booked for past dates. Please select today or a future date.");
                return;
            }
        } catch (DateTimeParseException e) {
            sendError(request, response, "Invalid date format. Please select a valid date.");
            return;
        }

        try {
            LocalDateTime appointmentDateTime = appointmentDate.atStartOfDay();
            Timestamp timestamp = Timestamp.valueOf(appointmentDateTime);
            String formattedSlotLabel = TimeSlotUtil.formatSlotLabel(timeStr);

            // =================================================================
            // DOCTOR & APPOINTMENT LOOKUP
            // =================================================================
            Doctor doctor = null;
            if (isReschedule) {
                int rescheduleId = Integer.parseInt(rescheduleIdParam.trim());
                Appointment existing = appointmentDAO.getAppointmentById(rescheduleId);

                if (existing == null) {
                    sendError(request, response, "Existing appointment record could not be found.");
                    return;
                }

                // Verify appointment ownership
                if (existing.getPatientId() != patientId) {
                    response.sendRedirect("patient-dashboard.jsp?error=unauthorized");
                    return;
                }

                if (!"SCHEDULED".equalsIgnoreCase(existing.getStatus())) {
                    sendError(request, response, "Only active scheduled appointments can be rescheduled.");
                    return;
                }
                doctor = doctorDAO.getDoctorById(existing.getDoctorId());
            } else {
                if (doctorIdParam != null && !doctorIdParam.trim().isEmpty()) {
                    try {
                        int docId = Integer.parseInt(doctorIdParam.trim());
                        doctor = doctorDAO.getDoctorById(docId);
                    } catch (NumberFormatException ignored) {}
                }

                if (doctor == null && doctorName != null) {
                    for (Doctor d : doctorDAO.getAllDoctors()) {
                        if (d.getName().equalsIgnoreCase(doctorName.trim())) {
                            doctor = d;
                            break;
                        }
                    }
                }
            }

            if (doctor == null) {
                sendError(request, response, "Doctor record could not be found.");
                return;
            }

            // 1. Weekday validation: chosen date's weekday must match doctor's AVAILABLE_DAYS
            if (!isDoctorAvailableOnDay(doctor, appointmentDate)) {
                String dayName = appointmentDate.getDayOfWeek().name().substring(0, 1) + 
                                 appointmentDate.getDayOfWeek().name().substring(1).toLowerCase();
                sendError(request, response, "Dr. " + doctor.getName() + " is not available on " + dayName + "s. Available days: " + doctor.getAvailableDays());
                return;
            }

            // 2. Time slot validation: slot must be in doctor's generated 30-min slots
            java.util.List<String> validSlots = TimeSlotUtil.generateTimeSlots(doctor.getAvailableTime(), 30);
            if (!validSlots.contains(timeStr)) {
                sendError(request, response, "Selected time slot (" + formattedSlotLabel + ") is outside Dr. " + doctor.getName() + "'s consultation hours (" + doctor.getAvailableTime() + ").");
                return;
            }

            // 3. Past time slot validation: if booking/rescheduling for today, reject if time has passed
            if (appointmentDate.isEqual(LocalDate.now())) {
                try {
                    java.time.LocalTime slotTime = java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    if (slotTime.isBefore(java.time.LocalTime.now())) {
                        sendError(request, response, "Cannot book a time slot that has already passed today. Please choose an upcoming time slot.");
                        return;
                    }
                } catch (Exception e) {
                    sendError(request, response, "Invalid time slot format.");
                    return;
                }
            }

            // =================================================================
            // RESCHEDULING FLOW
            // =================================================================
            if (isReschedule) {
                int rescheduleId = Integer.parseInt(rescheduleIdParam.trim());

                // Conflict check against other appointments for this doctor
                if (appointmentDAO.isSlotBookedExcept(doctor.getId(), timestamp, timeStr, rescheduleId)) {
                    sendError(request, response, "The slot " + formattedSlotLabel + " on " + appointmentDate + 
                            " is already booked for Dr. " + doctor.getName() + ". Please select another time slot or date.");
                    return;
                }

                // Conflict check for patient (SRS Section 8 Constraint: Patients cannot book multiple appointments for same slot)
                if (appointmentDAO.isPatientSlotBookedExcept(patientId, timestamp, timeStr, rescheduleId)) {
                    sendError(request, response, "You already have another appointment scheduled for " + 
                            formattedSlotLabel + " on " + appointmentDate + ". Patients cannot book multiple appointments for the same time slot.");
                    return;
                }

                boolean success = appointmentDAO.rescheduleAppointment(rescheduleId, timestamp, timeStr);
                if (success) {
                    LOGGER.info("Appointment ID=" + rescheduleId + " rescheduled to " + appointmentDate + " at " + timeStr);
                    Appointment updatedAppt = appointmentDAO.getAppointmentById(rescheduleId);
                    if (updatedAppt != null) {
                        NotificationService.getInstance().sendBookingConfirmationAsync(updatedAppt);
                    }
                    response.sendRedirect(request.getContextPath() + "/patient-dashboard.jsp?msg=rescheduled&id=" + rescheduleId);
                    return;
                } else {
                    sendError(request, response, "Unable to reschedule appointment. The slot may already be booked.");
                    return;
                }
            }

            // =================================================================
            // INITIAL BOOKING FLOW
            // =================================================================
            Patient patient = patientDAO.getPatientById(patientId);
            if (patient == null) {
                sendError(request, response, "Patient record could not be found.");
                return;
            }

            // Conflict check for doctor + date + time slot
            if (appointmentDAO.isSlotBooked(doctor.getId(), timestamp, timeStr)) {
                sendError(request, response, "Dr. " + doctor.getName() + " is already booked for " + 
                        formattedSlotLabel + " on " + appointmentDate + ". Please choose a different time slot or date.");
                return;
            }

            // Conflict check for patient + date + time slot (SRS Section 8 Constraint)
            if (appointmentDAO.isPatientSlotBooked(patient.getId(), timestamp, timeStr)) {
                sendError(request, response, "You already have an appointment scheduled for " + 
                        formattedSlotLabel + " on " + appointmentDate + ". Patients cannot book multiple appointments for the same time slot.");
                return;
            }

            // Create and persist appointment
            Appointment appointment = new Appointment(
                    doctor.getId(),
                    patient.getId(),
                    timestamp,
                    "SCHEDULED",
                    timeStr,
                    "UNPAID"
            );

            int appointmentId = appointmentDAO.bookAppointment(appointment);

            if (appointmentId > 0) {
                appointment.setId(appointmentId);
                appointment.setDoctorName(doctor.getName());
                appointment.setPatientName(patient.getName());
                NotificationService.getInstance().sendBookingConfirmationAsync(appointment);

                LOGGER.info("Appointment booked successfully. ID=" + appointmentId + 
                            ", Doctor=" + doctor.getName() + ", Time=" + timeStr + ", Patient=" + patient.getName());
                response.sendRedirect(request.getContextPath() + "/patient-dashboard.jsp?msg=booked&id=" + appointmentId);
            } else if (appointmentId == -2) {
                // Caught ORA-00001 unique index violation (doctor or patient slot clash)
                sendError(request, response, "The slot " + formattedSlotLabel + " on " + 
                        appointmentDate + " is already booked. Please choose a different time slot or date.");
            } else {
                sendError(request, response, "Selected time slot is already booked, or an unexpected error occurred.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing appointment booking/rescheduling", e);
            sendError(request, response, "A system error occurred while processing the appointment. Please try again.");
        }
    }

    private boolean isDoctorAvailableOnDay(Doctor doctor, LocalDate date) {
        if (doctor == null || doctor.getAvailableDays() == null || doctor.getAvailableDays().trim().isEmpty()) {
            return true;
        }
        String dayAbbr;
        switch (date.getDayOfWeek()) {
            case MONDAY: dayAbbr = "MON"; break;
            case TUESDAY: dayAbbr = "TUE"; break;
            case WEDNESDAY: dayAbbr = "WED"; break;
            case THURSDAY: dayAbbr = "THU"; break;
            case FRIDAY: dayAbbr = "FRI"; break;
            case SATURDAY: dayAbbr = "SAT"; break;
            case SUNDAY: dayAbbr = "SUN"; break;
            default: dayAbbr = "";
        }
        String[] days = doctor.getAvailableDays().split(",");
        for (String d : days) {
            if (d.trim().equalsIgnoreCase(dayAbbr)) {
                return true;
            }
        }
        return false;
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/appointment.jsp").forward(request, response);
    }
}
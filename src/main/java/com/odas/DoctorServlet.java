package com.odas;

import com.odas.dao.DoctorDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DoctorServlet handles retrieving and displaying the directory of medical professionals.
 * 
 * Purpose (for viva):
 * - Replaces static PrintWriter HTML with dynamic MVC pattern.
 * - Retrieves doctor records from Oracle DOCTORS table via DoctorDAO.
 * - Forwards data models to doctors.jsp view for presentation.
 */
@WebServlet(urlPatterns = {"/doctors", "/doctor-schedule"})
public class DoctorServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DoctorServlet.class.getName());
    private DoctorDAO doctorDAO;

    @Override
    public void init() throws ServletException {
        this.doctorDAO = new DoctorDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String name = request.getParameter("name");
        String specialization = request.getParameter("specialization");
        String location = request.getParameter("location");
        String availableDay = request.getParameter("availableDay");

        try {
            List<Doctor> doctors = doctorDAO.searchDoctors(name, specialization, location, availableDay);
            request.setAttribute("doctors", doctors);
            request.setAttribute("searchName", name != null ? name.trim() : "");
            request.setAttribute("searchSpecialization", specialization != null ? specialization.trim() : "");
            request.setAttribute("searchLocation", location != null ? location.trim() : "");
            request.setAttribute("searchAvailableDay", availableDay != null ? availableDay.trim() : "");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve doctors list from database", e);
            request.setAttribute("doctors", Collections.emptyList());
            request.setAttribute("errorMessage", "Unable to load doctors at this time. Please try again later.");
        }

        // Forward to the dynamic doctor listing view
        request.getRequestDispatcher("/doctors.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");
        if (!"DOCTOR".equalsIgnoreCase(role)) {
            response.sendRedirect("index.jsp?error=unauthorized");
            return;
        }

        Integer doctorId = (Integer) session.getAttribute("doctorId");
        if (doctorId == null) {
            Integer userId = (Integer) session.getAttribute("userId");
            Doctor doc = doctorDAO.getDoctorByUserId(userId);
            if (doc != null) {
                doctorId = doc.getId();
                session.setAttribute("doctorId", doctorId);
            }
        }

        if (doctorId == null) {
            response.sendRedirect("doctor-dashboard.jsp?error=unauthorized");
            return;
        }

        String availableDays = request.getParameter("availableDays");
        String availableTime = request.getParameter("availableTime");

        // Validate using ValidationUtil
        if (!com.odas.util.ValidationUtil.isValidAvailableDays(availableDays)) {
            response.sendRedirect("doctor-dashboard.jsp?error=" + 
                    java.net.URLEncoder.encode("Available days must be comma-separated days from Mon-Sun (e.g. 'Mon,Wed,Fri').", java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        if (!com.odas.util.ValidationUtil.isValidAvailableTime(availableTime)) {
            response.sendRedirect("doctor-dashboard.jsp?error=" + 
                    java.net.URLEncoder.encode("Available time must be in 24-hr 'HH:mm-HH:mm' format with start before end (e.g. '09:00-13:00').", java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        boolean updated = doctorDAO.updateDoctorSchedule(doctorId, availableDays, availableTime);
        if (updated) {
            LOGGER.info("Doctor ID=" + doctorId + " updated schedule to " + availableDays + " (" + availableTime + ")");
            response.sendRedirect("doctor-dashboard.jsp?msg=schedule_updated");
        } else {
            response.sendRedirect("doctor-dashboard.jsp?error=" + 
                    java.net.URLEncoder.encode("Failed to update schedule. Please try again.", java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
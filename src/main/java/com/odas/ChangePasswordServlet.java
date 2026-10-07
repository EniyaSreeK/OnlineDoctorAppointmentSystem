package com.odas;

import com.odas.dao.UserDAO;
import com.odas.util.PasswordUtil;
import com.odas.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChangePasswordServlet enables password rotation for all system roles (PATIENT, DOCTOR, ADMIN).
 * 
 * Purpose (for viva):
 * - Authenticates the user session.
 * - Validates current password against stored SHA-256 hash.
 * - Enforces security constraints: min 6 chars, new password != current password, new == confirm.
 * - Persists updated SHA-256 hash to the Oracle USERS table.
 */
@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChangePasswordServlet.class.getName());
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        request.getRequestDispatcher("/change-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        User user = userDAO.findById(userId);
        if (user == null) {
            response.sendRedirect("login.jsp?error=unauthorized");
            return;
        }

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // Validate password change parameters
        String validationError = ValidationUtil.validatePasswordChange(
                currentPassword, user.getPassword(), newPassword, confirmPassword);

        if (validationError != null) {
            request.setAttribute("errorMessage", validationError);
            request.getRequestDispatcher("/change-password.jsp").forward(request, response);
            return;
        }

        try {
            String newHashedPassword = PasswordUtil.hashPassword(newPassword);
            boolean updated = userDAO.updatePassword(userId, newHashedPassword);

            if (updated) {
                LOGGER.info("Password updated successfully for username=" + user.getUsername() + ", role=" + user.getRole());
                response.sendRedirect("change-password.jsp?msg=updated");
            } else {
                request.setAttribute("errorMessage", "Failed to update password due to a database issue. Please try again.");
                request.getRequestDispatcher("/change-password.jsp").forward(request, response);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating password for userId=" + userId, e);
            request.setAttribute("errorMessage", "A system error occurred while updating your password.");
            request.getRequestDispatcher("/change-password.jsp").forward(request, response);
        }
    }
}

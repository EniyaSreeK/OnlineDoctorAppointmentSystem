package com.odas.util;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Authentication and Authorization Filter.
 * 
 * Purpose (for viva):
 * - Intercepts requests to protected dashboards (/patient-dashboard.jsp, /doctor-dashboard.jsp, /admin-dashboard.jsp).
 * - Enforces role-based access control (RBAC).
 * - Redirects unauthenticated or unauthorized users to the login page.
 */
@WebFilter(urlPatterns = {
    "/patient-dashboard.jsp",
    "/patient-dashboard",
    "/doctor-dashboard.jsp",
    "/doctor-dashboard",
    "/admin-dashboard.jsp",
    "/admin-dashboard",
    "/appointment.jsp",
    "/appointment",
    "/appointment-action",
    "/patient-profile.jsp",
    "/profile",
    "/change-password.jsp",
    "/change-password",
    "/payment",
    "/payment.jsp",
    "/payment-receipt.jsp",
    "/prescription",
    "/prescription-form.jsp",
    "/prescription-view.jsp",
    "/admin-doctors",
    "/admin-patients",
    "/doctor-schedule"
})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Prevent browser caching of protected pages
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setDateHeader("Expires", 0);

        HttpSession session = httpRequest.getSession(false);
        String path = httpRequest.getServletPath();
        if (path == null) {
            path = httpRequest.getRequestURI();
        }

        boolean loggedIn = (session != null && session.getAttribute("userId") != null && session.getAttribute("role") != null);

        if (!loggedIn) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?error=unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");

        // Role-based route protection
        // 1. Admin endpoints
        if ((path.startsWith("/admin-dashboard") || path.startsWith("/admin-doctors") || path.startsWith("/admin-patients"))
                && !"ADMIN".equals(role)) {
            redirectToRoleHome(httpRequest, httpResponse, role);
            return;
        }

        // 2. Doctor endpoints
        if ((path.startsWith("/doctor-dashboard") || path.startsWith("/doctor-schedule") || path.equals("/prescription-form.jsp"))
                && !"DOCTOR".equals(role)) {
            redirectToRoleHome(httpRequest, httpResponse, role);
            return;
        }

        // 3. Patient endpoints
        if ((path.startsWith("/patient-dashboard") || path.startsWith("/patient-profile") || path.equals("/profile")
                || path.startsWith("/appointment.jsp") || path.equals("/appointment") || path.equals("/payment.jsp"))
                && !"PATIENT".equals(role)) {
            redirectToRoleHome(httpRequest, httpResponse, role);
            return;
        }

        chain.doFilter(request, response);
    }

    private void redirectToRoleHome(HttpServletRequest req, HttpServletResponse resp, String role) throws IOException {
        String ctx = req.getContextPath();
        if ("PATIENT".equals(role)) {
            resp.sendRedirect(ctx + "/patient-dashboard.jsp");
        } else if ("DOCTOR".equals(role)) {
            resp.sendRedirect(ctx + "/doctor-dashboard.jsp");
        } else if ("ADMIN".equals(role)) {
            resp.sendRedirect(ctx + "/admin-dashboard.jsp");
        } else {
            resp.sendRedirect(ctx + "/login.jsp");
        }
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}

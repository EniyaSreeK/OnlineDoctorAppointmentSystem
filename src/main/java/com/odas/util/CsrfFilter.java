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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CsrfFilter guards against Cross-Site Request Forgery (CSRF).
 * 
 * Purpose (for viva):
 * - Maintains a cryptographically secure token per user session using SecureRandom.
 * - Injects the CSRF token into request and session attributes.
 * - Intercepts all state-changing HTTP POST requests.
 * - Exempts public authentication endpoints (/login, /signup).
 * - Verifies submitted token using time-constant equality check (MessageDigest.isEqual).
 * - Rejects unauthorized requests with HTTP 403 Forbidden.
 */
@WebFilter(urlPatterns = "/*")
public class CsrfFilter implements Filter {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String CSRF_PARAM_NAME = "csrfToken";
    public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    /**
     * Generates a cryptographically strong, URL-safe random CSRF token string.
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No custom filter initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Ensure session exists and has a CSRF token
        HttpSession session = httpRequest.getSession(true);
        String sessionToken = (String) session.getAttribute(CSRF_PARAM_NAME);
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            sessionToken = generateToken();
            session.setAttribute(CSRF_PARAM_NAME, sessionToken);
        }
        httpRequest.setAttribute(CSRF_PARAM_NAME, sessionToken);

        // Check if request is a POST
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String servletPath = httpRequest.getServletPath();
            if (servletPath == null) {
                servletPath = "";
            }

            // Exempt login and signup endpoints from CSRF verification
            boolean isExempt = servletPath.equals("/login") || servletPath.equals("/signup")
                    || servletPath.endsWith("/login") || servletPath.endsWith("/signup");

            if (!isExempt) {
                String requestToken = httpRequest.getParameter(CSRF_PARAM_NAME);
                if (requestToken == null || requestToken.trim().isEmpty()) {
                    requestToken = httpRequest.getHeader(CSRF_HEADER_NAME);
                }

                if (requestToken == null || !isValidToken(sessionToken, requestToken)) {
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Constant-time comparison between session token and client-submitted token.
     */
    private boolean isValidToken(String sessionToken, String requestToken) {
        if (sessionToken == null || requestToken == null) {
            return false;
        }
        byte[] a = sessionToken.getBytes(StandardCharsets.UTF_8);
        byte[] b = requestToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    @Override
    public void destroy() {
        // Clean up resources if necessary
    }
}

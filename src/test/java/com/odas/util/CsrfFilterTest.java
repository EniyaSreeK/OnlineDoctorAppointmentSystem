package com.odas.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for CsrfFilter token generation, post request interception,
 * token verification, and 403 Forbidden rejection.
 */
public class CsrfFilterTest {

    private CsrfFilter filter;

    @Before
    public void setUp() {
        this.filter = new CsrfFilter();
    }

    @Test
    public void testTokenGeneration() {
        String token1 = CsrfFilter.generateToken();
        String token2 = CsrfFilter.generateToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals("Each generated token must be unique", token1, token2);
        assertTrue("Token must be non-empty", token1.length() >= 32);
    }

    @Test
    public void testPostWithoutTokenRejectedWith403() throws IOException, ServletException {
        MockEnvironment env = new MockEnvironment("POST", "/appointment-action");
        env.sessionAttributes.put(CsrfFilter.CSRF_PARAM_NAME, "valid-session-token");
        // No request parameter provided

        filter.doFilter(env.request, env.response, env.chain);

        assertEquals("Missing token on POST must result in 403 Forbidden", 403, env.responseStatusCode.get());
        assertFalse("FilterChain should NOT proceed on missing CSRF token", env.chainCalled.get());
    }

    @Test
    public void testPostWithMismatchedTokenRejectedWith403() throws IOException, ServletException {
        MockEnvironment env = new MockEnvironment("POST", "/appointment-action");
        env.sessionAttributes.put(CsrfFilter.CSRF_PARAM_NAME, "valid-session-token");
        env.parameters.put(CsrfFilter.CSRF_PARAM_NAME, "tampered-attacker-token");

        filter.doFilter(env.request, env.response, env.chain);

        assertEquals("Mismatched token on POST must result in 403 Forbidden", 403, env.responseStatusCode.get());
        assertFalse("FilterChain should NOT proceed on invalid CSRF token", env.chainCalled.get());
    }

    @Test
    public void testPostWithValidTokenProceeds() throws IOException, ServletException {
        MockEnvironment env = new MockEnvironment("POST", "/appointment-action");
        String goodToken = "matching-secure-token-12345";
        env.sessionAttributes.put(CsrfFilter.CSRF_PARAM_NAME, goodToken);
        env.parameters.put(CsrfFilter.CSRF_PARAM_NAME, goodToken);

        filter.doFilter(env.request, env.response, env.chain);

        assertEquals("Response code should remain unset (0)", 0, env.responseStatusCode.get());
        assertTrue("FilterChain must be invoked when CSRF token matches", env.chainCalled.get());
    }

    @Test
    public void testPostWithValidHeaderTokenProceeds() throws IOException, ServletException {
        MockEnvironment env = new MockEnvironment("POST", "/payment");
        String goodToken = "header-token-xyz";
        env.sessionAttributes.put(CsrfFilter.CSRF_PARAM_NAME, goodToken);
        env.headers.put(CsrfFilter.CSRF_HEADER_NAME, goodToken);

        filter.doFilter(env.request, env.response, env.chain);

        assertTrue("FilterChain must be invoked when header CSRF token matches", env.chainCalled.get());
    }

    @Test
    public void testExemptEndpointsPassWithoutToken() throws IOException, ServletException {
        // /login endpoint
        MockEnvironment loginEnv = new MockEnvironment("POST", "/login");
        filter.doFilter(loginEnv.request, loginEnv.response, loginEnv.chain);
        assertTrue("/login should be exempt from CSRF token check", loginEnv.chainCalled.get());

        // /signup endpoint
        MockEnvironment signupEnv = new MockEnvironment("POST", "/signup");
        filter.doFilter(signupEnv.request, signupEnv.response, signupEnv.chain);
        assertTrue("/signup should be exempt from CSRF token check", signupEnv.chainCalled.get());
    }

    @Test
    public void testGetRequestsPassThroughWithoutToken() throws IOException, ServletException {
        MockEnvironment getEnv = new MockEnvironment("GET", "/doctors");
        filter.doFilter(getEnv.request, getEnv.response, getEnv.chain);
        assertTrue("GET requests must pass through without CSRF validation", getEnv.chainCalled.get());
    }

    // =========================================================================
    // Lightweight Mock Harness
    // =========================================================================

    private static class MockEnvironment {
        final Map<String, Object> sessionAttributes = new HashMap<>();
        final Map<String, Object> requestAttributes = new HashMap<>();
        final Map<String, String> parameters = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        final AtomicInteger responseStatusCode = new AtomicInteger(0);
        final AtomicBoolean chainCalled = new AtomicBoolean(false);

        final HttpServletRequest request;
        final HttpServletResponse response;
        final FilterChain chain;

        MockEnvironment(String method, String servletPath) {
            HttpSession sessionProxy = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        if ("getAttribute".equals(name)) return sessionAttributes.get(args[0]);
                        if ("setAttribute".equals(name)) { sessionAttributes.put((String) args[0], args[1]); return null; }
                        if ("getId".equals(name)) return "mock-session-1";
                        return null;
                    }
            );

            this.request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        if ("getMethod".equals(name)) return method;
                        if ("getServletPath".equals(name)) return servletPath;
                        if ("getRequestURI".equals(name)) return "/context" + servletPath;
                        if ("getParameter".equals(name)) return parameters.get(args[0]);
                        if ("getHeader".equals(name)) return headers.get(args[0]);
                        if ("getSession".equals(name)) return sessionProxy;
                        if ("getAttribute".equals(name)) return requestAttributes.get(args[0]);
                        if ("setAttribute".equals(name)) { requestAttributes.put((String) args[0], args[1]); return null; }
                        return null;
                    }
            );

            this.response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        if ("sendError".equals(name)) {
                            responseStatusCode.set((Integer) args[0]);
                            return null;
                        }
                        if ("setStatus".equals(name)) {
                            responseStatusCode.set((Integer) args[0]);
                            return null;
                        }
                        return null;
                    }
            );

            this.chain = (req, res) -> chainCalled.set(true);
        }
    }
}

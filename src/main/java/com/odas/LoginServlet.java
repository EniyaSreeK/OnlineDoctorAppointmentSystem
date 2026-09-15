package com.odas;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        if (username.equals("admin") && password.equals("1234")) {
            out.println("<h1>Login Successful</h1>");
            out.println("<p>Welcome, " + username + "!</p>");
        } else {
            out.println("<h1>Invalid Login</h1>");
            out.println("<p>Username or password is incorrect.</p>");
        }
    }
}
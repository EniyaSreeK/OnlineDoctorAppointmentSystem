package com.odas.notify;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Email sender using Jakarta Mail over SMTP with STARTTLS.
 * 
 * Purpose (for viva):
 * - Dispatches transactional email notifications to registered patients.
 * - Enforces TLS encryption (STARTTLS) on SMTP communication.
 * - Degrades gracefully if environment variables are absent (logs INFO, never crashes).
 * - Never throws runtime exceptions; catches all errors to prevent application disruption.
 */
public class EmailSender {

    private static final Logger LOGGER = Logger.getLogger(EmailSender.class.getName());

    private final String host;
    private final String port;
    private final String user;
    private final String password;

    public EmailSender() {
        this(
            getEnvOrProperty("ODAS_SMTP_HOST"),
            getEnvOrProperty("ODAS_SMTP_PORT"),
            getEnvOrProperty("ODAS_SMTP_USER"),
            getEnvOrProperty("ODAS_SMTP_PASSWORD")
        );
    }

    public EmailSender(String host, String port, String user, String password) {
        this.host = host;
        this.port = (port != null && !port.trim().isEmpty()) ? port.trim() : "587";
        this.user = user;
        this.password = password;
    }

    /**
     * Checks if mandatory SMTP settings are configured.
     */
    public boolean isConfigured() {
        return host != null && !host.trim().isEmpty() &&
               user != null && !user.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    /**
     * Sends an email over SMTP with STARTTLS.
     * 
     * @param toEmail destination email address
     * @param subject email subject line
     * @param body plain text body
     * @return true if sent successfully, false otherwise
     */
    public boolean send(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }

        if (!isConfigured()) {
            LOGGER.info("SMTP configuration not provided (ODAS_SMTP_HOST, ODAS_SMTP_USER, ODAS_SMTP_PASSWORD). Skipping email dispatch to " + toEmail);
            return false;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.host", host.trim());
            props.put("mail.smtp.port", port.trim());
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user.trim(), password.trim());
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user.trim(), "MediCare ODAS"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail.trim()));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");

            Transport.send(message);
            LOGGER.info("Email notification successfully dispatched to " + toEmail.trim());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send email to " + toEmail + ": " + e.getMessage(), e);
            return false;
        }
    }

    private static String getEnvOrProperty(String name) {
        String val = System.getenv(name);
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty(name);
        }
        return val != null ? val.trim() : null;
    }
}

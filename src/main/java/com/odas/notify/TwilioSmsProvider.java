package com.odas.notify;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Twilio SMS delivery provider communicating via Twilio REST API using java.net.http.HttpClient.
 * 
 * Purpose (for viva):
 * - Communicates with Twilio Messages endpoint using Basic Authentication.
 * - Formats 10-digit Indian phone numbers with E.164 international prefix (+91).
 * - Avoids external SDK dependencies by using Java 11+ standard HTTP client.
 * - Never throws runtime exceptions; returns false and logs errors upon failure.
 */
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger LOGGER = Logger.getLogger(TwilioSmsProvider.class.getName());

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final HttpClient httpClient;

    public TwilioSmsProvider() {
        this(
            getEnvOrProperty("TWILIO_ACCOUNT_SID"),
            getEnvOrProperty("TWILIO_AUTH_TOKEN"),
            getEnvOrProperty("TWILIO_FROM_NUMBER"),
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        );
    }

    public TwilioSmsProvider(String accountSid, String authToken, String fromNumber) {
        this(accountSid, authToken, fromNumber, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public TwilioSmsProvider(String accountSid, String authToken, String fromNumber, HttpClient httpClient) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.httpClient = (httpClient != null) ? httpClient : HttpClient.newHttpClient();
    }

    @Override
    public boolean send(String toPhone, String message) {
        if (toPhone == null || toPhone.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            LOGGER.warning("Twilio SMS dispatch skipped: missing phone number or message body.");
            return false;
        }

        if (accountSid == null || accountSid.trim().isEmpty() ||
            authToken == null || authToken.trim().isEmpty() ||
            fromNumber == null || fromNumber.trim().isEmpty()) {
            LOGGER.warning("Twilio credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER) not fully configured.");
            return false;
        }

        String formattedPhone = formatPhoneNumber(toPhone);

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid.trim() + "/Messages.json";

            String formData = "To=" + URLEncoder.encode(formattedPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber.trim(), StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String auth = accountSid.trim() + ":" + authToken.trim();
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.info("Twilio SMS dispatched successfully to " + formattedPhone);
                return true;
            } else {
                LOGGER.warning("Twilio SMS failed with HTTP " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Twilio SMS dispatch interrupted for " + formattedPhone, e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error dispatching Twilio SMS to " + formattedPhone, e);
            return false;
        }
    }

    /**
     * Formats 10-digit phone numbers with +91 country code prefix if missing.
     */
    public static String formatPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }
        String clean = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
        if (clean.startsWith("+")) {
            return clean;
        }
        if (clean.length() == 10 && clean.matches("^\\d{10}$")) {
            return "+91" + clean;
        }
        if (clean.length() == 12 && clean.startsWith("91")) {
            return "+" + clean;
        }
        return clean;
    }

    private static String getEnvOrProperty(String name) {
        String val = System.getenv(name);
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty(name);
        }
        return val != null ? val.trim() : null;
    }
}

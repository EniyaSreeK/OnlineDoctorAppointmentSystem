package com.odas.notify;

/**
 * Interface for SMS delivery providers in ODAS.
 */
public interface SmsProvider {

    /**
     * Sends an SMS message to the specified recipient phone number.
     * 
     * @param toPhone recipient phone number
     * @param message SMS body text
     * @return true if the SMS was successfully queued or dispatched, false otherwise
     */
    boolean send(String toPhone, String message);
}

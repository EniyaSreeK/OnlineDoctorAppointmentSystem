package com.odas.notify;

import java.util.logging.Logger;

/**
 * Mock SMS provider for development and testing environments.
 * Simulates successful SMS dispatch by logging the outgoing payload.
 */
public class MockSmsProvider implements SmsProvider {

    private static final Logger LOGGER = Logger.getLogger(MockSmsProvider.class.getName());

    @Override
    public boolean send(String toPhone, String message) {
        LOGGER.info("[MOCK SMS] to=" + toPhone + " msg=" + message);
        return true;
    }
}

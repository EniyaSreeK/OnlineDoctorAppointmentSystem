package com.odas.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for PBKDF2WithHmacSHA256 password hashing and legacy SHA-256 upgrade support.
 */
public class PasswordUtilTest {

    @Test
    public void testHashFormat() {
        String password = "StrongPassword@123";
        String hash = PasswordUtil.hashPassword(password);

        assertNotNull("Hash should not be null", hash);
        assertTrue("Hash must start with pbkdf2 prefix", hash.startsWith(PasswordUtil.PREFIX));

        // Format is: pbkdf2$iterations$saltB64$hashB64
        String[] parts = hash.split("\\$");
        assertEquals("PBKDF2 hash should have 4 parts separated by $", 4, parts.length);
        assertEquals("pbkdf2", parts[0]);
        assertEquals("210000", parts[1]);
        assertFalse("Salt must not be empty", parts[2].trim().isEmpty());
        assertFalse("Hash must not be empty", parts[3].trim().isEmpty());
    }

    @Test
    public void testSaltUniqueness() {
        String password = "SamePassword456!";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals("Each hash must have a unique random salt", hash1, hash2);
        assertTrue("Both hashes must verify the original password", PasswordUtil.checkPassword(password, hash1));
        assertTrue("Both hashes must verify the original password", PasswordUtil.checkPassword(password, hash2));
    }

    @Test
    public void testCheckPasswordMatchesAndRejections() {
        String password = "CorrectSecretPassword#99";
        String hash = PasswordUtil.hashPassword(password);

        assertTrue("Valid password must verify", PasswordUtil.checkPassword(password, hash));
        assertFalse("Incorrect password must fail", PasswordUtil.checkPassword("WrongSecretPassword#99", hash));
        assertFalse("Empty password must fail", PasswordUtil.checkPassword("", hash));
        assertFalse("Null password must fail", PasswordUtil.checkPassword(null, hash));
        assertFalse("Null hash must fail", PasswordUtil.checkPassword(password, null));
        assertFalse("Empty hash must fail", PasswordUtil.checkPassword(password, ""));
    }

    @Test
    public void testLegacySha256VerificationAndUpgrade() {
        // SHA-256 of "admin123" = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
        String legacyHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";

        assertTrue("Legacy SHA-256 hash must verify correctly", PasswordUtil.checkPassword("admin123", legacyHash));
        assertFalse("Legacy SHA-256 must reject wrong password", PasswordUtil.checkPassword("wrong123", legacyHash));

        // Test upgrade detection
        assertTrue("Legacy hash should be flagged for upgrade", PasswordUtil.needsUpgrade(legacyHash));

        String modernHash = PasswordUtil.hashPassword("admin123");
        assertFalse("Modern PBKDF2 hash should NOT need upgrade", PasswordUtil.needsUpgrade(modernHash));
    }

    @Test
    public void testMalformedHashHandling() {
        assertFalse("Malformed hash prefix should fail safely", PasswordUtil.checkPassword("password", "pbkdf2$invalid"));
        assertFalse("Non-numeric iterations should fail safely", PasswordUtil.checkPassword("password", "pbkdf2$abc$salt$hash"));
        assertFalse("Corrupt base64 should fail safely", PasswordUtil.checkPassword("password", "pbkdf2$210000$???$???"));
    }
}
